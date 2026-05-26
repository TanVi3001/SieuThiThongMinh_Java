package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.sync.SyncVersionDao;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RealtimeNotifier chuẩn: - Publish đúng event, đúng module. - Giữ tương thích
 * event cũ để không phá các panel hiện tại. - Dedupe event trùng trong thời
 * gian ngắn để tránh reload bừa. - Dashboard/Report được debounce. -
 * AuditLog/LoginHistory không nằm trong realtime.
 */
public final class RealtimeNotifier {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.realtime");
    private static final long DUPLICATE_WINDOW_MS = 350L;

    private static final ScheduledExecutorService SCHEDULER
            = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "realtime-notifier-debounce");
                t.setDaemon(true);
                return t;
            });

    private static final Object SUMMARY_LOCK = new Object();
    private static final Map<String, Long> LAST_EVENT_AT = new ConcurrentHashMap<>();

    private static boolean dashboardPending = false;
    private static boolean reportPending = false;

    private RealtimeNotifier() {
    }

    public static void productsChanged(String message) {
        notify("PRODUCTS", AppEventType.PRODUCT_CHANGED, AppEventType.PRODUCTS, "PRODUCTS_CHANGED", message);
        scheduleDashboard("FROM_PRODUCTS");
    }

    public static void inventoryChanged(String message) {
        notify("INVENTORY", AppEventType.INVENTORY_CHANGED, AppEventType.INVENTORY, "INVENTORY_CHANGED", message);
        scheduleDashboard("FROM_INVENTORY");
    }

    public static void suppliersChanged(String message) {
        notify("SUPPLIERS", AppEventType.SUPPLIER_CHANGED, null, "SUPPLIER_CHANGED", message);
    }

    public static void customersChanged(String message) {
        notify("CUSTOMERS", AppEventType.CUSTOMER_CHANGED, AppEventType.CUSTOMERS, "CUSTOMERS_CHANGED", message);
        scheduleDashboard("FROM_CUSTOMERS");
    }

    public static void employeesChanged(String message) {
        notify("EMPLOYEES", AppEventType.EMPLOYEE_CHANGED, AppEventType.EMPLOYEES, "EMPLOYEES_CHANGED", message);
        scheduleDashboard("FROM_EMPLOYEES");
    }

    public static void shiftChanged(String message) {
        notify("EMPLOYEE_SHIFT_ASSIGNMENTS", AppEventType.SHIFT_CHANGED, null, "SHIFT_CHANGED", message);
    }

    public static void ordersChanged(String message) {
        notify("ORDERS", AppEventType.ORDER_CHANGED, AppEventType.ORDERS, "ORDERS_CHANGED", message);
        scheduleDashboard("FROM_ORDERS");
        scheduleReport("FROM_ORDERS");
    }

    /**
     * Compatibility cho code cũ. Chi tiết đơn hàng vẫn thuộc nhóm
     * ORDER_CHANGED. Không tạo event riêng để tránh OrderView/Report reload 2
     * lần.
     */
    public static void orderDetailsChanged(String message) {
        ordersChanged("ORDER_DETAILS_CHANGED:" + normalize(message, "ORDER_DETAILS_CHANGED"));
    }

    public static void promotionsChanged(String message) {
        notify("PROMOTIONS", AppEventType.PROMOTION_CHANGED, null, "PROMOTION_CHANGED", message);
    }

    public static void storesChanged(String message) {
        notify("STORES", AppEventType.STORE_CHANGED, AppEventType.STORE_INFO, "STORE_CHANGED", message);
        scheduleDashboard("FROM_STORES");
    }

    public static void accountChanged(String message) {
        notify("ACCOUNTS", AppEventType.ACCOUNT_CHANGED, AppEventType.ACCOUNT_SECURITY, "ACCOUNT_CHANGED", message);
    }

    public static void roleChanged(String message) {
        notify("ROLES", AppEventType.ROLE_CHANGED, AppEventType.ACCOUNT_SECURITY, "ROLE_CHANGED", message);
    }

    public static void accountSecurityChanged(String message) {
        accountChanged(message);
    }

    public static void systemConfigChanged(String message) {
        notify("SYSTEM_CONFIG", AppEventType.SYSTEM_CONFIG_CHANGED, AppEventType.SYSTEM_CONFIG, "SYSTEM_CONFIG_CHANGED", message);
        scheduleDashboard("FROM_SYSTEM_CONFIG");
    }

    public static void dashboardChanged(String message) {
        String msg = normalize(message, "DASHBOARD_CHANGED");
        publish(AppEventType.DASHBOARD_CHANGED, msg);
        publish(AppEventType.DASHBOARD, msg);
        sendRemote("DASHBOARD_CHANGED:" + msg);
    }

    public static void reportChanged(String message) {
        String msg = normalize(message, "REPORT_CHANGED");
        publish(AppEventType.REPORT_CHANGED, msg);
        publish(AppEventType.STATISTICS, msg);
        sendRemote("REPORT_CHANGED:" + msg);
    }

    public static void statisticsChanged(String message) {
        reportChanged(message);
    }

    public static void inventoryAlert(String message) {
        String msg = normalize(message, "INVENTORY_ALERT");
        publish(AppEventType.INVENTORY_ALERT, msg);
        sendRemote("INVENTORY_ALERT:" + msg);
    }

    private static void notify(
            String syncKey,
            AppEventType newType,
            AppEventType legacyType,
            String remotePrefix,
            String message
    ) {
        String msg = normalize(message, remotePrefix);
        String dedupeKey = remotePrefix + "|" + msg;

        if (isDuplicate(dedupeKey)) {
            return;
        }

        bump(syncKey);

        publish(newType, msg);

        if (legacyType != null && legacyType != newType) {
            publish(legacyType, msg);
        }

        sendRemote(remotePrefix + ":" + msg);
    }

    private static boolean isDuplicate(String key) {
        long now = System.currentTimeMillis();
        Long previous = LAST_EVENT_AT.put(key, now);

        if (previous == null) {
            return false;
        }

        return now - previous < DUPLICATE_WINDOW_MS;
    }

    private static void publish(AppEventType type, String message) {
        try {
            EventBus.publish(new AppDataChangedEvent(type, message));
        } catch (Exception e) {
            System.err.println("[RealtimeNotifier] publish error: " + e.getMessage());
        }
    }

    private static void bump(String key) {
        try {
            SyncVersionDao.bumpVersion(key);
        } catch (Exception e) {
            System.err.println("[RealtimeNotifier] bump error " + key + ": " + e.getMessage());
        }
    }

    private static void sendRemote(String message) {
        try {
            if (DEBUG_LOG) {
                System.out.println("[RealtimeNotifier] " + message);
            }
            RealtimeClient.send(message);
        } catch (Exception e) {
            System.err.println("[RealtimeNotifier] remote send error: " + e.getMessage());
        }
    }

    private static void scheduleDashboard(String reason) {
        synchronized (SUMMARY_LOCK) {
            if (dashboardPending) {
                return;
            }

            dashboardPending = true;
            SCHEDULER.schedule(() -> {
                synchronized (SUMMARY_LOCK) {
                    dashboardPending = false;
                }
                dashboardChanged(reason);
            }, 900, TimeUnit.MILLISECONDS);
        }
    }

    private static void scheduleReport(String reason) {
        synchronized (SUMMARY_LOCK) {
            if (reportPending) {
                return;
            }

            reportPending = true;
            SCHEDULER.schedule(() -> {
                synchronized (SUMMARY_LOCK) {
                    reportPending = false;
                }
                reportChanged(reason);
            }, 1200, TimeUnit.MILLISECONDS);
        }
    }

    private static String normalize(String message, String fallback) {
        return message == null || message.trim().isEmpty()
                ? fallback
                : message.trim();
    }
}
