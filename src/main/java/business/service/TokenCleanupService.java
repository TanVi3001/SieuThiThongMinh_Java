package business.service;

import business.sql.rbac.TokenSql;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TokenCleanupService {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.tokenCleanup");
    private static final long INITIAL_DELAY_MINUTES = 10L;
    private static final long CLEANUP_INTERVAL_MINUTES = 60L;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-cleanup-thread");
        t.setDaemon(true);
        return t;
    });

    private static boolean started = false;

    private TokenCleanupService() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }

        started = true;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                int deleted = TokenSql.getInstance().deleteExpiredTokens();
                if (DEBUG_LOG && deleted > 0) {
                    System.out.println("[TokenCleanupService] deleted expired tokens = " + deleted);
                }
            } catch (Exception e) {
                System.err.println("[TokenCleanupService] cleanup error: " + e.getMessage());
            }
        }, INITIAL_DELAY_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public static void stop() {
        scheduler.shutdownNow();
    }
}
