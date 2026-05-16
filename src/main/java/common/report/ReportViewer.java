package common.report;

import common.db.DatabaseConnection;
import java.awt.BorderLayout;
import java.io.File;
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

    public ReportViewer(String jrxmlPath, HashMap<String, Object> parameters) {
        this(jrxmlPath, (Map<String, Object>) parameters);
    }

    public ReportViewer(String jrxmlPath, Map<String, Object> parameters) {
        setTitle("Trình xem báo cáo");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1000, 700);
        setLocationRelativeTo(null);

        loadReport(jrxmlPath, parameters == null ? new HashMap<>() : parameters);
    }

    public static void showReport(String jrxmlPath, HashMap<String, Object> parameters) {
        SwingUtilities.invokeLater(() -> new ReportViewer(jrxmlPath, parameters).setVisible(true));
    }

    private void loadReport(String jrxmlPath, Map<String, Object> parameters) {
        File reportFile = new File(jrxmlPath);
        if (!reportFile.isFile()) {
            showError("Khong tim thay file report: " + reportFile.getAbsolutePath());
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (connection == null) {
                showError("Khong the ket noi database. Kiem tra cau hinh Oracle trong DatabaseConnection.");
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(reportFile.getAbsolutePath());
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, parameters, connection);
            add(new JRViewer(jasperPrint), BorderLayout.CENTER);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Loi khi mo report: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Report Error", JOptionPane.ERROR_MESSAGE);
    }
}
