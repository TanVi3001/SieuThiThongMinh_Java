package business.service;

import business.sql.rbac.AccountSql;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HeartbeatService giu session dang nhap hien tai con song.
 *
 * Chu ky thuc te cho desktop noi bo:
 * - Heartbeat moi 20 giay: du nhanh de cap nhat online/offline, khong spam DB.
 * - Cleanup dead session moi 2 phut: tranh quet bang lien tuc khi nhieu may cung mo app.
 */
public class HeartbeatService {

    private static final long INITIAL_DELAY_SECONDS = 2L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 20L;
    private static final long CLEANUP_INTERVAL_MS = 120_000L;

    private static ScheduledExecutorService scheduler;

    private static String currentAccountId;
    private static String currentSessionId;
    private static boolean stoppedByLogout = false;
    private static long lastCleanupAt = 0L;

    private HeartbeatService() {
    }

    public static synchronized void start(String accountId, String sessionId) {
        stopOnlyScheduler();

        currentAccountId = clean(accountId);
        currentSessionId = clean(sessionId);
        stoppedByLogout = false;
        lastCleanupAt = 0L;

        if (currentAccountId == null || currentSessionId == null) {
            System.err.println("[HeartbeatService] Cannot start because accountId/sessionId is missing.");
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

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isStoppedOrInvalidSession()) {
                    return;
                }

                AccountSql accountSql = AccountSql.getInstance();
                cleanupDeadSessionsIfNeeded(accountSql);

                /*
                 * Check trạng thái tài khoản trước.
                 * Nếu Admin khóa tài khoản thì status sẽ là Bị khóa => đá ra.
                 * Nếu tài khoản vẫn Hoạt động thì tuyệt đối không đá ra chỉ vì CURRENT_SESSION_ID
                 * trong bảng ACCOUNTS chưa đồng bộ với ACCOUNT_SESSIONS.
                 */
                if (!isAccountLoginAllowed(currentAccountId)) {
                    stopOnlyScheduler();
                    common.security.SecurityGuard.forceLogoutCurrentSession(
                            "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.\n"
                            + "Vui lòng liên hệ quản trị viên nếu cần mở lại tài khoản."
                    );
                    return;
                }

                boolean sessionAlive = accountSql.heartbeatSession(
                        currentAccountId,
                        currentSessionId
                );

                if (!sessionAlive) {
                    /*
                     * Session có thể bị EXPIRED do đứng máy/debug/cleanup trước heartbeat.
                     * Nếu account vẫn Hoạt động thì tạo lại đúng session hiện tại và tiếp tục.
                     * Không gọi isCurrentSessionValid() ở đây nữa vì hàm đó đang dựa vào
                     * ACCOUNTS.CURRENT_SESSION_ID, trong khi flow mới lưu session chính ở
                     * ACCOUNT_SESSIONS. Chính điểm này làm Admin/Staff bị văng nhầm.
                     */
                    boolean recreated = accountSql.createLoginSession(currentAccountId, currentSessionId);
                    boolean recovered = recreated && accountSql.heartbeatSession(currentAccountId, currentSessionId);

                    if (!recovered) {
                        stopOnlyScheduler();
                        common.security.SecurityGuard.forceLogoutCurrentSession(
                                "Phiên đăng nhập không còn hợp lệ.\nVui lòng đăng nhập lại."
                        );
                        return;
                    }
                }

                accountSql.heartbeat(currentAccountId);

            } catch (Exception e) {
                System.err.println("[HeartbeatService] heartbeat error: " + e.getMessage());
            }
        }, INITIAL_DELAY_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

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

    private static void cleanupDeadSessionsIfNeeded(AccountSql accountSql) throws Exception {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupAt = now;
        accountSql.cleanupDeadSessions();
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

    private static boolean isAccountLoginAllowed(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        String sql = """
            SELECT NVL(is_deleted, 0) AS is_deleted,
                   NVL(status, N'Hoạt động') AS account_status
            FROM ACCOUNTS
            WHERE account_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                int isDeleted = rs.getInt("is_deleted");
                String status = rs.getString("account_status");
                return isDeleted == 0 && !isLockedStatus(status);
            }

        } catch (Exception e) {
            System.err.println("[HeartbeatService] isAccountLoginAllowed error: " + e.getMessage());
            return false;
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

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
