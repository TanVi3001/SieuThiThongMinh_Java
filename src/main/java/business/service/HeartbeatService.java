package business.service;

import java.awt.Window;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Heartbeat kiểm tra phiên đăng nhập hiện tại.
 *
 * Logic Force Logout: - Một tài khoản chỉ có 1 phiên hợp lệ tại một thời điểm.
 * - Nếu tài khoản đăng nhập ở máy khác, session cũ sẽ bị kick về Login. - Chặn
 * lỗi hiện 2 popup bằng SecurityGuard + stoppedByLogout.
 */
public class HeartbeatService {

    private static ScheduledExecutorService scheduler;
    private static String currentAccountId;
    private static String currentSessionId;
    private static boolean stoppedByLogout = false;

    private HeartbeatService() {
    }

    public static synchronized void start(String accountId, String sessionId) {
        stopOnlyScheduler();

        stoppedByLogout = false;
        currentAccountId = accountId;
        currentSessionId = sessionId;

        // Mỗi lần bắt đầu session mới thì mở lại quyền xử lý logout
        try {
            common.security.SecurityGuard.setProcessingLogout(false);
        } catch (Exception ignored) {
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "account-heartbeat-thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isStoppedOrInvalidSession()) {
                    return;
                }

                boolean valid = AccountService.heartbeatAndCheckSession(
                        currentAccountId,
                        currentSessionId
                );

                AccountService.cleanupDeadSessions();

                if (!valid) {
                    forceLogoutBecauseNewLogin();
                }

            } catch (Exception e) {
                System.err.println("[HeartbeatService] heartbeat error: " + e.getMessage());
            }
        }, 10, 5, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        stoppedByLogout = true;
        stopOnlyScheduler();
        currentAccountId = null;
        currentSessionId = null;
    }

    private static synchronized void stopOnlyScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    public static synchronized boolean markLogoutOnce() {
        if (stoppedByLogout) {
            return false;
        }

        stoppedByLogout = true;
        return true;
    }

    public static synchronized String getCurrentAccountId() {
        return currentAccountId;
    }

    public static synchronized String getCurrentSessionId() {
        return currentSessionId;
    }

    private static synchronized boolean isStoppedOrInvalidSession() {
        return stoppedByLogout
                || currentAccountId == null
                || currentAccountId.trim().isEmpty()
                || currentSessionId == null
                || currentSessionId.trim().isEmpty();
    }

    private static void forceLogoutBecauseNewLogin() {
        synchronized (HeartbeatService.class) {
            if (stoppedByLogout || common.security.SecurityGuard.isProcessingLogout()) {
                return;
            }

            stoppedByLogout = true;
            common.security.SecurityGuard.setProcessingLogout(true);
        }

        stopOnlyScheduler();

        SwingUtilities.invokeLater(() -> {
            try {
                JOptionPane optionPane = new JOptionPane(
                        "Tài khoản của bạn đã được đăng nhập ở thiết bị khác.\n"
                        + "Phiên hiện tại sẽ được đăng xuất để bảo mật.",
                        JOptionPane.WARNING_MESSAGE
                );

                JDialog dialog = optionPane.createDialog(null, "Phiên đăng nhập đã bị thay thế");
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setModal(true);
                dialog.setVisible(true);
                dialog.dispose();

                business.service.SessionManager.clear();

                try {
                    common.auth.UserSession.getInstance().clear();
                } catch (Exception ignored) {
                }

                for (java.awt.Window w : java.awt.Window.getWindows()) {
                    if (w != null && w.isDisplayable()) {
                        w.dispose();
                    }
                }

                view.LoginView login = new view.LoginView();
                login.setLocationRelativeTo(null);
                login.setVisible(true);

                // KHÔNG reset false ở đây nếu Dashboard cũ còn timer.
                // Reset false ở LoginView khi người dùng bấm đăng nhập lại.
            } catch (Exception e) {
                System.err.println("[HeartbeatService] force logout UI error: " + e.getMessage());
                System.exit(0);
            }
        });
    }

    private static void showForceLogoutDialog() {
        JOptionPane optionPane = new JOptionPane(
                "Tài khoản của bạn đã được đăng nhập ở thiết bị khác.\n"
                + "Phiên hiện tại sẽ được đăng xuất để bảo mật.",
                JOptionPane.WARNING_MESSAGE
        );

        JDialog dialog = optionPane.createDialog(null, "Phiên đăng nhập đã bị thay thế");
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.dispose();
    }
}
