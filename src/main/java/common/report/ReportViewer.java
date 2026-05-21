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
        setSize(1000, 700);
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
        try (Connection connection = DatabaseConnection.getConnection();
             InputStream reportStream = openReportStream(reportPath)) {

            if (connection == null) {
                showError("Không thể kết nối database. Kiểm tra cấu hình Oracle trong DatabaseConnection.");
                return;
            }

            if (reportStream == null) {
                showError("Không tìm thấy file report: " + reportPath
                        + "\nĐặt file .jrxml trong src/main/resources/reports và gọi bằng /reports/TenFile.jrxml");
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
            showError("Lỗi khi mở report: " + ex.getMessage());
        }
    }

    private InputStream openReportStream(String reportPath) throws Exception {
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return null;
        }

        String normalizedPath = reportPath.trim().replace('\\', '/');

        InputStream stream = openClasspathResource(normalizedPath);
        if (stream != null) {
            return stream;
        }

        // Fallback cho code cũ đang truyền dạng src/main/resources/reports/File.jrxml
        String resourcesPrefix = "src/main/resources";
        int idx = normalizedPath.indexOf(resourcesPrefix);
        if (idx >= 0) {
            String resourcePath = normalizedPath.substring(idx + resourcesPrefix.length());
            stream = openClasspathResource(resourcePath);
            if (stream != null) {
                return stream;
            }
        }

        File reportFile = new File(reportPath);
        if (reportFile.isFile()) {
            return new FileInputStream(reportFile);
        }

        return null;
    }

    private InputStream openClasspathResource(String path) {
        String resourcePath = path.startsWith("/") ? path : "/" + path;
        InputStream stream = ReportViewer.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1));
        }
        return stream;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Report Error", JOptionPane.ERROR_MESSAGE);
    }
}
