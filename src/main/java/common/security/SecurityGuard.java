package common.security;

import business.service.LoginService;
import common.db.DatabaseConnection;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import model.account.Account;
import view.LoginView;

public class SecurityGuard {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.security");

    // GLOBAL GUARD FLAG: Cờ khóa chống gọi Logout nhiều lần
    private static volatile boolean isProcessingLogout = false;

    public static boolean isProcessingLogout() {
        return isProcessingLogout;
    }

    public static void setProcessingLogout(boolean value) {
        isProcessingLogout = value;
    }

    public static void attach(JPanel view) {
        isProcessingLogout = false;

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (isProcessingLogout || event == null || event.getType() == null) {
                return;
            }

            if (event.getType() == AppEventType.ACCOUNT_SECURITY) {
                debug("[SecurityGuard] ACCOUNT_SECURITY received");
                verifyCurrentSession(view);
            }
        });
    }

    /**
     * Dùng cho HeartbeatService khi phát hiện session bị force logout / tài khoản bị khóa.
     * Hàm này public để mọi portal tự văng về Login khi Admin khóa tài khoản từ máy khác.
     */
    public static void forceLogoutCurrentSession(String message) {
        if (isProcessingLogout) {
            return;
        }

        isProcessingLogout = true;
        SwingUtilities.invokeLater(() -> forceLogout(null, message));
    }

    private static void verifyCurrentSession(JPanel view) {
        if (isProcessingLogout) {
            return;
        }

        Account currentUser = business.service.SessionManager.getCurrentUser();

        if (currentUser == null) {
            currentUser = LoginService.getCurrentUser();
        }

        if (currentUser == null || currentUser.getAccountId() == null) {
            return;
        }

        String accId = currentUser.getAccountId();
        String currentRole = business.service.SessionManager.getCurrentRole();

        if (currentRole == null || currentRole.trim().isEmpty()) {
            if (currentUser.getRoleId() != null && !currentUser.getRoleId().trim().isEmpty()) {
                currentRole = currentUser.getRoleId();
            } else if (currentUser.getRoleValue() != null && !currentUser.getRoleValue().trim().isEmpty()) {
                currentRole = currentUser.getRoleValue();
            } else {
                currentRole = currentUser.getRole();
            }
        }

        final String finalCurrentRole = currentRole;

        new Thread(() -> {
            try {
                AccountSecurityState latestData = loadAccountSecurityState(accId);

                if (latestData == null) {
                    return;
                }

                boolean roleChanged = latestData.roleId == null
                        || finalCurrentRole == null
                        || !latestData.roleId.trim().equalsIgnoreCase(finalCurrentRole.trim());

                if (!latestData.loginAllowed || roleChanged) {
                    if (!isProcessingLogout) {
                        isProcessingLogout = true;
                        debug("[SecurityGuard] KICK USER currentRole=" + finalCurrentRole
                                + ", dbRoleId=" + latestData.roleId
                                + ", status=" + latestData.status
                                + ", isDeleted=" + latestData.isDeleted);

                        String message = !latestData.loginAllowed
                                ? "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.\nVui lòng liên hệ quản trị viên."
                                : "Quyền truy cập của bạn đã thay đổi.\nVui lòng đăng nhập lại để cập nhật!";

                        SwingUtilities.invokeLater(() -> forceLogout(view, message));
                    }
                }
            } catch (Exception e) {
                System.err.println("SecurityGuard Error: " + e.getMessage());
            }
        }, "security-guard-check-thread").start();
    }

    private static AccountSecurityState loadAccountSecurityState(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT a.account_id,
                   COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value,
                   NVL(a.is_deleted, 0) AS is_deleted,
                   NVL(a.status, N'Hoạt động') AS account_status
            FROM ACCOUNTS a
            LEFT JOIN ACCOUNT_ASSIGN_ROLE aar
                   ON a.account_id = aar.account_id
                  AND NVL(aar.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg
                   ON a.account_id = aarg.account_id
                  AND NVL(aarg.is_deleted, 0) = 0
            LEFT JOIN ROLE_GROUPS rg
                   ON aarg.role_group_id = rg.role_group_id
                  AND NVL(rg.is_deleted, 0) = 0
            WHERE a.account_id = ?
            FETCH FIRST 1 ROWS ONLY
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                AccountSecurityState state = new AccountSecurityState();
                state.roleId = rs.getString("role_value");
                state.isDeleted = rs.getInt("is_deleted");
                state.status = rs.getString("account_status");
                state.loginAllowed = state.isDeleted == 0 && !isLockedStatus(state.status);
                return state;
            }

        } catch (Exception e) {
            System.err.println("[SecurityGuard] loadAccountSecurityState error: " + e.getMessage());
            return null;
        }
    }

    private static boolean isLockedStatus(String status) {
        if (status == null) {
            return false;
        }

        String s = status.trim().toUpperCase();
        return s.equals("BỊ KHÓA")
                || s.equals("BI KHOA")
                || s.equals("LOCKED")
                || s.equals("DISABLED")
                || s.equals("INACTIVE")
                || s.equals("TẠM KHÓA")
                || s.equals("TAM KHOA");
    }

    private static void forceLogout(JPanel view, String message) {
        String finalMessage = message == null || message.trim().isEmpty()
                ? "Quyền truy cập của bạn đã thay đổi hoặc tài khoản đã bị khóa.\nVui lòng đăng nhập lại để cập nhật!"
                : message;

        try {
            JOptionPane.showMessageDialog(
                    view,
                    finalMessage,
                    "Cảnh báo bảo mật",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (Exception ignored) {
        }

        try {
            LoginService.logout();
        } catch (Exception e) {
            System.err.println("[SecurityGuard] LoginService.logout error: " + e.getMessage());
        }

        try {
            business.service.SessionManager.clear();
        } catch (Exception ignored) {
        }

        try {
            common.auth.UserSession.getInstance().clear();
        } catch (Exception ignored) {
        }

        try {
            for (Window w : Window.getWindows()) {
                if (w == null) {
                    continue;
                }

                if (w instanceof LoginView) {
                    continue;
                }

                w.setVisible(false);
                w.dispose();
            }
        } catch (Exception e) {
            System.err.println("[SecurityGuard] dispose windows error: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof LoginView && w.isDisplayable()) {
                    w.setVisible(true);
                    w.toFront();
                    w.requestFocus();
                    return;
                }
            }

            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
            login.toFront();
            login.requestFocus();
        });
    }

    private static class AccountSecurityState {
        String roleId;
        String status;
        int isDeleted;
        boolean loginAllowed;
    }

    private static void debug(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }
}
