package business.service;

import business.sql.rbac.AccountSql;
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

                boolean updated = accountSql.heartbeatSession(
                        currentAccountId,
                        currentSessionId
                );

                if (!updated) {
                    accountSql.createLoginSession(currentAccountId, currentSessionId);
                    accountSql.heartbeatSession(currentAccountId, currentSessionId);
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

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
