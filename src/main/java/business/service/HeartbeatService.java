package business.service;

import business.sql.rbac.AccountSql;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HeartbeatService dùng để giữ phiên đăng nhập hiện tại còn sống.
 *
 * Logic mới: - Cho phép tài khoản Manager đăng nhập nhiều phiên. - Mỗi cửa
 * sổ/thiết bị có một session_id riêng trong ACCOUNT_SESSIONS. - Heartbeat chỉ
 * cập nhật last_heartbeat_at cho session hiện tại. - Không kick phiên cũ khi
 * login phiên mới. - Session chết quá 30 giây sẽ được cleanup thành EXPIRED.
 *
 * Việc kick user khi đổi role/quyền sẽ do SecurityGuard xử lý qua
 * ACCOUNT_SECURITY_CHANGED.
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

        currentAccountId = clean(accountId);
        currentSessionId = clean(sessionId);
        stoppedByLogout = false;

        if (currentAccountId == null || currentSessionId == null) {
            System.err.println("[HeartbeatService] Không thể start vì thiếu accountId/sessionId.");
            return;
        }

        try {
            common.security.SecurityGuard.setProcessingLogout(false);
        } catch (Exception ignored) {
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "account-heartbeat-thread");
            t.setDaemon(true);
            return t;
        });

        /*
         * Chạy ngay sau 3 giây để UI online cập nhật nhanh.
         * Sau đó mỗi 5 giây update heartbeat.
         */
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isStoppedOrInvalidSession()) {
                    return;
                }

                AccountSql accountSql = AccountSql.getInstance();

                /*
                 * Dọn session chết trước để bảng nhân viên không còn Online ảo.
                 */
                accountSql.cleanupDeadSessions();

                /*
                 * Cập nhật heartbeat cho đúng session hiện tại.
                 */
                boolean updated = accountSql.heartbeatSession(
                        currentAccountId,
                        currentSessionId
                );

                /*
                 * Nếu vì lý do nào đó session chưa tồn tại trong ACCOUNT_SESSIONS
                 * thì tạo lại để tránh UI bị Offline dù app đang mở.
                 */
                if (!updated) {
                    accountSql.createLoginSession(currentAccountId, currentSessionId);
                    accountSql.heartbeatSession(currentAccountId, currentSessionId);
                }

                /*
                 * Optional: đồng bộ trạng thái ACCOUNTS để các màn cũ còn dùng
                 * ONLINE_STATUS/LAST_HEARTBEAT_AT vẫn không bị sai.
                 */
                accountSql.heartbeat(currentAccountId);

            } catch (Exception e) {
                System.err.println("[HeartbeatService] heartbeat error: " + e.getMessage());
            }
        }, 3, 5, TimeUnit.SECONDS);
    }

    /**
     * Gọi khi user đăng xuất chủ động. Sẽ đóng đúng session hiện tại trong
     * ACCOUNT_SESSIONS.
     */
    public static synchronized void stop() {
        stoppedByLogout = true;

        String accountId = currentAccountId;
        String sessionId = currentSessionId;

        stopOnlyScheduler();

        if (accountId != null && sessionId != null) {
            try {
                AccountSql.getInstance().closeLoginSession(accountId, sessionId);
                AccountSql.getInstance().cleanupDeadSessions();
            } catch (Exception e) {
                System.err.println("[HeartbeatService] close session error: " + e.getMessage());
            }
        }

        currentAccountId = null;
        currentSessionId = null;
    }

    /**
     * Chỉ dừng timer, không đóng session. Dùng trong một số luồng force
     * logout/đóng app đã tự xử lý DB bên ngoài.
     */
    private static synchronized void stopOnlyScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = null;
    }

    /**
     * Dùng để tránh gọi logout/close session nhiều lần.
     */
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

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
