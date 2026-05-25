package business.service;

import common.db.DatabaseConnection;
import model.account.Account;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import javax.swing.JDialog;
import javax.swing.Timer;

public class AuditLogService {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.audit");

    public static void logAction(String actionType, String entityType, String entityId, String oldValue, String newValue, String reason) {
        Account currentUser = LoginService.getCurrentUser();
        String accountId = (currentUser != null) ? currentUser.getAccountId() : null;

        String logId = "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        String sql = "INSERT INTO AUDIT_LOG (LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID, OLD_VALUE, NEW_VALUE, REASON) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, logId);
            ps.setString(2, accountId);
            ps.setString(3, actionType);
            ps.setString(4, entityType);
            ps.setString(5, entityId);
            ps.setString(6, oldValue);
            ps.setString(7, newValue);
            ps.setString(8, reason);

            ps.executeUpdate();

            if (DEBUG_LOG) {
                System.out.println("[AuditLog] Saved: " + actionType + " on " + entityType);
            }

            autoHideRoleSuccessDialog(actionType, entityType);

        } catch (Exception e) {
            System.err.println("[AuditLog] Lỗi ghi nhật ký hệ thống: " + e.getMessage());
        }
    }

    private static void autoHideRoleSuccessDialog(String actionType, String entityType) {
        if (!"CẬP NHẬT".equalsIgnoreCase(String.valueOf(actionType).trim())
                || !"ACCOUNTS".equalsIgnoreCase(String.valueOf(entityType).trim())) {
            return;
        }

        final int[] ticks = {0};
        Timer timer = new Timer(80, null);
        timer.addActionListener(e -> {
            ticks[0]++;

            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog
                        && dialog.isShowing()
                        && "Thành công".equals(dialog.getTitle())) {
                    dialog.setVisible(false);
                }
            }

            if (ticks[0] >= 20) {
                timer.stop();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
}