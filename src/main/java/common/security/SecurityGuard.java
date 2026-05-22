package common.security;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.rbac.AccountSql;
import business.service.LoginService;
import model.account.Account;
import view.LoginView;
import javax.swing.*;
import java.awt.Window;

public class SecurityGuard {

    // 🌟 GLOBAL GUARD FLAG: Cờ khóa chống gọi Logout nhiều lần (Chống 2 app, 2 popup)
    private static volatile boolean isProcessingLogout = false;

    // 👉 THÊM VÀO: Hàm để các file khác có thể ĐỌC được trạng thái cờ
    public static boolean isProcessingLogout() {
        return isProcessingLogout;
    }

    // 👉 ĐÃ SỬA: Hàm để các file khác có thể ĐỔI trạng thái cờ (Xóa cái ném lỗi của IDE đi)
    public static void setProcessingLogout(boolean value) {
        isProcessingLogout = value;
    }

    public static void attach(JPanel view) {
        // Reset cờ mỗi khi gắn guard mới (lúc login vào)
        isProcessingLogout = false;

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            // Nếu đang trong quá trình văng acc rồi thì bỏ qua mọi event khác
            if (isProcessingLogout) {
                return;
            }

            if (event.getType() == AppEventType.ACCOUNT_SECURITY) {
                System.out.println("🛡️ [SecurityGuard] Bắt được tín hiệu WebSocket đổi quyền!");
                verifyCurrentSession(view);
            }
        });
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
                String[] latestData = AccountSql.getInstance().getAccountDetails(accId);

                if (latestData == null) {
                    return;
                }

                String dbRoleId = latestData[4];
                boolean isActive = "0".equals(String.valueOf(latestData[5]).trim());

                boolean roleChanged = dbRoleId == null
                        || finalCurrentRole == null
                        || !dbRoleId.trim().equalsIgnoreCase(finalCurrentRole.trim());

                if (!isActive || roleChanged) {
                    if (!isProcessingLogout) {
                        isProcessingLogout = true;

                        System.out.println("🛡️ [SecurityGuard] 🚨 PHÁT HIỆN ĐỔI QUYỀN");
                        System.out.println("🛡️ [SecurityGuard] currentRole=" + finalCurrentRole + ", dbRoleId=" + dbRoleId);
                        System.out.println("🛡️ [SecurityGuard] -> KICK USER");

                        SwingUtilities.invokeLater(() -> forceLogout(view));
                    }
                }
            } catch (Exception e) {
                System.err.println("SecurityGuard Error: " + e.getMessage());
            }
        }, "security-guard-check-thread").start();
    }

    private static void forceLogout(JPanel view) {
        try {
            JOptionPane.showMessageDialog(
                    view,
                    "Quyền truy cập của bạn đã thay đổi hoặc tài khoản đã bị khóa.\n"
                    + "Vui lòng đăng nhập lại để cập nhật!",
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

        /*
     * QUAN TRỌNG:
     * Không chỉ dispose window ancestor của panel.
     * Phải đóng toàn bộ JFrame dashboard đang mở để tránh Warehouse Portal còn nằm phía sau.
         */
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
            /*
         * Nếu đã có LoginView đang mở thì chỉ focus lại, không tạo thêm.
             */
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

    private static boolean isCurrentAccountRoleChanged() {
        try {
            model.account.Account currentUser = business.service.SessionManager.getCurrentUser();

            if (currentUser == null || currentUser.getAccountId() == null) {
                return false;
            }

            String[] latestData = business.sql.rbac.AccountSql.getInstance()
                    .getAccountDetails(currentUser.getAccountId());

            if (latestData == null) {
                return true;
            }

            String dbRoleId = latestData[4];

            String currentRole = business.service.SessionManager.getCurrentRole();

            if (dbRoleId == null || currentRole == null) {
                return true;
            }

            return !dbRoleId.trim().equalsIgnoreCase(currentRole.trim());

        } catch (Exception e) {
            System.err.println("[SecurityGuard] Cannot check current role: " + e.getMessage());
            return false;
        }
    }
}
