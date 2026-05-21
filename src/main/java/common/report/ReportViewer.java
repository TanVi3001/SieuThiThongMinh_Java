package common.report;

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
        SwingUtilities.invokeLater(() -> {
            ReportViewer viewer = new ReportViewer(reportPath, parameters);
            viewer.setVisible(true);
            viewer.toFront();
            viewer.requestFocus();
        });
    }

    private void loadReport(String reportPath, Map<String, Object> parameters) {
        try (Connection connection = DatabaseConnection.getConnection(); InputStream reportStream = openReportStream(reportPath)) {

            if (connection == null) {
                showError("Không thể kết nối database. Kiểm tra DatabaseConnection.");
                return;
            }

            if (reportStream == null) {
                showError(
                        "Không tìm thấy file report:\n" + reportPath
                        + "\n\nĐúng format nên là:"
                        + "\n/reports/SalesInvoiceReport.jrxml"
                        + "\n/reports/RevenueReport.jrxml"
                        + "\n/reports/PurchaseReceiptReport.jrxml"
                );
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, parameters, connection);

            getContentPane().removeAll();
            add(new JRViewer(jasperPrint), BorderLayout.CENTER);
            revalidate();
            repaint();

        } catch (Exception ex) {
            ex.printStackTrace();

            String rootMessage = getRootCauseMessage(ex);

            showError(
                    "Lỗi khi mở report:\n"
                    + rootMessage
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

        String resourcesPrefix = "src/main/resources";
        int idx = path.indexOf(resourcesPrefix);
        if (idx >= 0) {
            String resourcePath = path.substring(idx + resourcesPrefix.length());
            stream = openClasspathResource(resourcePath);
            if (stream != null) {
                return stream;
            }
        }

        File file = new File(reportPath);
        if (file.isFile()) {
            return new FileInputStream(file);
        }

        return null;
    }

    private InputStream openClasspathResource(String path) {
        String resourcePath = path.startsWith("/") ? path : "/" + path;

        InputStream stream = ReportViewer.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                stream = cl.getResourceAsStream(resourcePath.substring(1));
            }
        }

        return stream;
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
