package common.sync;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mỗi process app chạy 1 watcher. Watcher poll APP_SYNC; thấy version đổi =>
 * publish event => UI reload.
 */
public final class SyncWatcher {

    private static final ScheduledExecutorService SCHEDULER
            = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SyncWatcher");
                t.setDaemon(true);
                return t;
            });

    private static final Map<AppEventType, Long> LAST = new EnumMap<>(AppEventType.class);
    private static volatile boolean started = false;

    private SyncWatcher() {
    }

    public static void start(long periodSeconds) {
        if (started) {
            return;
        }
        started = true;

        long safePeriodSeconds = Math.max(1, periodSeconds);

        // init version cache
        for (AppEventType t : AppEventType.values()) {
            if (t == AppEventType.UNKNOWN) {
                continue;
            }
            LAST.put(t, readVersion(t));
        }

        SCHEDULER.scheduleAtFixedRate(SyncWatcher::tick, safePeriodSeconds, safePeriodSeconds, TimeUnit.SECONDS);
        System.out.println("[SYNC] SyncWatcher started. periodSeconds=" + safePeriodSeconds);
    }

    private static void tick() {
        try {
            for (AppEventType t : AppEventType.values()) {
                if (t == AppEventType.UNKNOWN) {
                    continue;
                }

                long current = readVersion(t);
                Long last = LAST.get(t);
                if (current >= 0 && last != null && current != last) {
                    LAST.put(t, current);
                    EventBus.publish(new AppDataChangedEvent(t, "DB changed: " + t + " v=" + current));
                }
            }
        } catch (Exception e) {
            System.err.println("[SYNC] tick error: " + e.getMessage());
        }
    }

    private static long readVersion(AppEventType type) {
        String key = mapSyncKey(type);
        if (key == null) {
            return -1;
        }
        return SyncVersionDao.getVersion(key);
    }

    private static String mapSyncKey(AppEventType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case PRODUCTS -> "PRODUCTS";
            case INVENTORY, INVENTORY_ALERT -> "INVENTORY";
            case CUSTOMERS -> "CUSTOMERS";
            case EMPLOYEES -> "EMPLOYEES";
            case ORDERS -> "ORDERS";
            case STATISTICS -> "STATISTICS";
            case DASHBOARD -> "DASHBOARD";
            case STORE_INFO -> "STORE_INFO";
            case SYSTEM_CONFIG -> "SYSTEM_CONFIG";
            case ACCOUNT_SECURITY -> "ACCOUNT_SECURITY";
            default -> null;
        };
    }
}