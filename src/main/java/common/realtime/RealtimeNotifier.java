package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.sync.SyncVersionDao;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Centralized realtime mapping helper.
 *
 * Rule for desktop production:
 * - Domain screens receive their primary realtime event immediately.
 * - Dashboard and Statistics still stay realtime, but companion refresh events are
 *   debounced so one payment does not trigger 10+ reloads.
 */
public final class RealtimeNotifier {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.realtime");
    private static final long SUMMARY_DEBOUNCE_MS = 900L;

    private static final ScheduledExecutorService SUMMARY_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "realtime-summary-debounce-thread");
        t.setDaemon(true);
        return t;
    });

    private static final Object SUMMARY_LOCK = new Object();
    private static ScheduledFuture<?> pendingDashboardTask;
    private static ScheduledFuture<?> pendingStatisticsTask;
    private static String pendingDashboardReason = "SUMMARY_UPDATED";
    private static String pendingStatisticsReason = "SUMMARY_UPDATED";

    private RealtimeNotifier() {
    }

    public static void ordersChanged(String message) {
        notify("ORDERS", AppEventType.ORDERS, "ORDERS_CHANGED", message, "ORDER_UPDATED");
        scheduleDashboardAndStatistics("FROM_ORDERS:" + normalize(message, "ORDER_UPDATED"));
    }

    public static void orderDetailsChanged(String message) {
        notify("ORDER_DETAILS", AppEventType.ORDERS, "ORDER_DETAILS_CHANGED", message, "ORDER_DETAILS_UPDATED");
        scheduleDashboardAndStatistics("FROM_ORDER_DETAILS:" + normalize(message, "ORDER_DETAILS_UPDATED"));
    }

    public static void inventoryChanged(String message) {
        notify("INVENTORY", AppEventType.INVENTORY, "INVENTORY_CHANGED", message, "STOCK_UPDATED");
        scheduleDashboardAndStatistics("FROM_INVENTORY:" + normalize(message, "STOCK_UPDATED"));
    }

    public static void productsChanged(String message) {
        notify("PRODUCTS", AppEventType.PRODUCTS, "PRODUCTS_CHANGED", message, "PRODUCT_UPDATED");
        scheduleDashboardAndStatistics("FROM_PRODUCTS:" + normalize(message, "PRODUCT_UPDATED"));
    }

    public static void customersChanged(String message) {
        notify("CUSTOMERS", AppEventType.CUSTOMERS, "CUSTOMERS_CHANGED", message, "CUSTOMER_UPDATED");
        scheduleDashboardAndStatistics("FROM_CUSTOMERS:" + normalize(message, "CUSTOMER_UPDATED"));
    }

    public static void employeesChanged(String message) {
        notify("EMPLOYEES", AppEventType.EMPLOYEES, "EMPLOYEES_CHANGED", message, "EMPLOYEE_UPDATED");
        scheduleDashboardAndStatistics("FROM_EMPLOYEES:" + normalize(message, "EMPLOYEE_UPDATED"));
    }

    public static void storesChanged(String message) {
        notify("STORES", AppEventType.STORE_INFO, "STORE_INFO_CHANGED", message, "STORE_UPDATED");
        scheduleDashboardAndStatistics("FROM_STORES:" + normalize(message, "STORE_UPDATED"));
    }

    public static void accountSecurityChanged(String message) {
        notify("ACCOUNTS", AppEventType.ACCOUNT_SECURITY, "ACCOUNT_SECURITY_CHANGED", message, "ACCOUNT_UPDATED");
        scheduleDashboardOnly("FROM_ACCOUNT_SECURITY:" + normalize(message, "ACCOUNT_UPDATED"));
    }

    public static void systemConfigChanged(String message) {
        notify("SYSTEM_CONFIG", AppEventType.SYSTEM_CONFIG, "SYSTEM_CONFIG_CHANGED", message, "SYSTEM_CONFIG_UPDATED");
        scheduleDashboardAndStatistics("FROM_SYSTEM_CONFIG:" + normalize(message, "SYSTEM_CONFIG_UPDATED"));
    }

    public static void dashboardChanged(String message) {
        notifyNoBump(AppEventType.DASHBOARD, "DASHBOARD_CHANGED", message, "DASHBOARD_UPDATED");
    }

    public static void statisticsChanged(String message) {
        notifyNoBump(AppEventType.STATISTICS, "STATISTICS_CHANGED", message, "STATISTICS_UPDATED");
    }

    public static void inventoryAlert(String message) {
        notifyNoBump(AppEventType.INVENTORY_ALERT, "INVENTORY_ALERT", message, "LOW_STOCK");
        scheduleDashboardOnly("FROM_INVENTORY_ALERT:" + normalize(message, "LOW_STOCK"));
    }

    private static void notify(String syncKey, AppEventType type, String remotePrefix, String message, String fallback) {
        bump(syncKey);
        String normalized = normalize(message, fallback);
        publishLocal(type, normalized);
        sendRemote(remotePrefix + ":" + normalized);
    }

    private static void notifyNoBump(AppEventType type, String remotePrefix, String message, String fallback) {
        String normalized = normalize(message, fallback);
        publishLocal(type, normalized);
        sendRemote(remotePrefix + ":" + normalized);
    }

    private static void scheduleDashboardAndStatistics(String reason) {
        scheduleDashboardOnly(reason);
        scheduleStatisticsOnly(reason);
    }

    private static void scheduleDashboardOnly(String reason) {
        synchronized (SUMMARY_LOCK) {
            pendingDashboardReason = normalize(reason, "SUMMARY_UPDATED");
            if (pendingDashboardTask != null && !pendingDashboardTask.isDone()) {
                pendingDashboardTask.cancel(false);
            }
            pendingDashboardTask = SUMMARY_SCHEDULER.schedule(
                    () -> dashboardChanged(pendingDashboardReason),
                    SUMMARY_DEBOUNCE_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private static void scheduleStatisticsOnly(String reason) {
        synchronized (SUMMARY_LOCK) {
            pendingStatisticsReason = normalize(reason, "SUMMARY_UPDATED");
            if (pendingStatisticsTask != null && !pendingStatisticsTask.isDone()) {
                pendingStatisticsTask.cancel(false);
            }
            pendingStatisticsTask = SUMMARY_SCHEDULER.schedule(
                    () -> statisticsChanged(pendingStatisticsReason),
                    SUMMARY_DEBOUNCE_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private static void bump(String key) {
        try {
            SyncVersionDao.bumpVersion(key);
        } catch (Exception ex) {
            System.err.println("[RealtimeNotifier] bump version error for " + key + ": " + ex.getMessage());
        }
    }

    private static void publishLocal(AppEventType type, String message) {
        try {
            EventBus.publish(new AppDataChangedEvent(type, message));
        } catch (Exception ex) {
            System.err.println("[RealtimeNotifier] local publish error: " + ex.getMessage());
        }
    }

    private static void sendRemote(String message) {
        try {
            if (DEBUG_LOG) {
                System.out.println("[RealtimeNotifier] send: " + message);
            }
            RealtimeClient.send(message);
        } catch (Exception ex) {
            System.err.println("[RealtimeNotifier] remote send error: " + ex.getMessage());
        }
    }

    private static String normalize(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }

        return message.trim();
    }
}
