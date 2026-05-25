package common.report;

import business.service.RolePermissionService;
import common.db.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.swing.JRViewer;

public class ReportViewer extends JFrame {

    public ReportViewer(String reportPath, HashMap<String, Object> parameters) {
        this(reportPath, (Map<String, Object>) parameters);
    }

    public ReportViewer(String reportPath, Map<String, Object> parameters) {
        setTitle("Trình xem báo cáo");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        setLayout(new BorderLayout());
        setSize(1100, 750);
        setLocationRelativeTo(null);

        loadReport(reportPath, parameters == null ? new HashMap<>() : parameters);
    }

    public static void showReport(String reportPath, HashMap<String, Object> parameters) {
        showReport(reportPath, (Map<String, Object>) parameters);
    }

    public static void showReport(String reportPath, Map<String, Object> parameters) {
        if (!RolePermissionService.canExport()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Bạn không có quyền Xuất file / Xuất báo cáo!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            ReportViewer viewer = new ReportViewer(reportPath, parameters);
            viewer.setVisible(true);
            viewer.toFront();
            viewer.requestFocus();
        });
    }

    private void loadReport(String reportPath, Map<String, Object> parameters) {
        try (
                Connection connection = DatabaseConnection.getConnection();
                InputStream reportStream = openReportStream(reportPath)
        ) {
            if (connection == null) {
                showError("Không thể kết nối database. Kiểm tra DatabaseConnection.");
                return;
            }

            if (reportStream == null) {
                showError(
                        "Không tìm thấy file report:\n" + reportPath
                        + "\n\nĐã thử load theo classpath và file system."
                        + "\n\nVị trí đúng của bạn nên là:"
                        + "\nsrc/main/resources/reports/SalesInvoiceReport.jrxml"
                        + "\nsrc/main/resources/reports/RevenueReport.jrxml"
                        + "\nsrc/main/resources/reports/PurchaseReceiptReport.jrxml"
                        + "\nsrc/main/resources/reports/AdminSystemReport.jrxml"
                );
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, parameters, connection);

            getContentPane().removeAll();
            add(new JRViewer(jasperPrint), BorderLayout.CENTER);
            revalidate();
            repaint();

        } catch (JRException ex) {
            ex.printStackTrace();

            showError(
                    "JasperReports không load/compile/fill được report.\n\n"
                    + "Nguyên nhân thường gặp:\n"
                    + "- Sai tên field trong file .jrxml so với SQL\n"
                    + "- Thiếu parameter như ORDER_ID, STORE_ID\n"
                    + "- Query trong .jrxml lỗi Oracle\n"
                    + "- File .jrxml dùng font hoặc resource không tồn tại\n\n"
                    + "Chi tiết lỗi:\n" + getRootCauseMessage(ex)
            );

        } catch (Exception ex) {
            ex.printStackTrace();

            showError(
                    "Lỗi khi mở report:\n"
                    + getRootCauseMessage(ex)
                    + "\n\nXem console để biết dòng lỗi chi tiết."
            );
        }
    }

    private InputStream openReportStream(String reportPath) throws Exception {
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return null;
        }

        String path = reportPath.trim().replace('\\', '/');

        InputStream stream = openClasspathResource(path);
        if (stream != null) {
            return stream;
        }

        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        String resourcesPrefix = "src/main/resources";
        int idx = cleanPath.indexOf(resourcesPrefix);
        if (idx >= 0) {
            String resourcePath = cleanPath.substring(idx + resourcesPrefix.length());
            stream = openClasspathResource(resourcePath);
            if (stream != null) {
                return stream;
            }
        }

        File directFile = new File(path);
        if (directFile.isFile()) {
            return new FileInputStream(directFile);
        }

        File cleanFile = new File(cleanPath);
        if (cleanFile.isFile()) {
            return new FileInputStream(cleanFile);
        }

        File mavenResourceFile = new File("src/main/resources/" + cleanPath);
        if (mavenResourceFile.isFile()) {
            return new FileInputStream(mavenResourceFile);
        }

        File projectResourceFile = findUpward(new File(System.getProperty("user.dir")), "src/main/resources/" + cleanPath);
        if (projectResourceFile != null && projectResourceFile.isFile()) {
            return new FileInputStream(projectResourceFile);
        }

        File targetClassesFile = new File("target/classes/" + cleanPath);
        if (targetClassesFile.isFile()) {
            return new FileInputStream(targetClassesFile);
        }

        File buildClassesFile = new File("build/classes/" + cleanPath);
        if (buildClassesFile.isFile()) {
            return new FileInputStream(buildClassesFile);
        }

        System.err.println("Không tìm thấy reportPath: " + reportPath);
        System.err.println("user.dir = " + System.getProperty("user.dir"));
        System.err.println("Đã thử:");
        System.err.println("- classpath: " + path);
        System.err.println("- file: " + directFile.getAbsolutePath());
        System.err.println("- file: " + cleanFile.getAbsolutePath());
        System.err.println("- file: " + mavenResourceFile.getAbsolutePath());
        System.err.println("- file: " + targetClassesFile.getAbsolutePath());
        System.err.println("- file: " + buildClassesFile.getAbsolutePath());

        return null;
    }

    private InputStream openClasspathResource(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String resourcePath = path.trim().replace('\\', '/');
        resourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        InputStream stream = ReportViewer.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            if (cl != null) {
                String withoutSlash = resourcePath.startsWith("/")
                        ? resourcePath.substring(1)
                        : resourcePath;

                stream = cl.getResourceAsStream(withoutSlash);
            }
        }

        return stream;
    }

    private File findUpward(File startDir, String relativePath) {
        File current = startDir;

        while (current != null) {
            File candidate = new File(current, relativePath);

            if (candidate.isFile()) {
                return candidate;
            }

            current = current.getParentFile();
        }

        return null;
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable root = ex;

        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = root.getClass().getName();
        }

        return message;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Report Error", JOptionPane.ERROR_MESSAGE);
    }
}