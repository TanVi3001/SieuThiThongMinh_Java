package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.sync.SyncVersionDao;

/**
 * Centralized realtime mapping helper.
 *
 * Production rule:
 * - Only publish the primary domain event.
 * - Do not cascade Dashboard/Statistics companion events for every small change.
 *   Those summary screens can reload when opened or through SyncWatcher.
 * - This prevents dozens of reloads after a single payment and keeps Swing UI smooth.
 */
public final class RealtimeNotifier {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.realtime");

    private RealtimeNotifier() {
    }

    public static void ordersChanged(String message) {
        notify("ORDERS", AppEventType.ORDERS, "ORDERS_CHANGED", message, "ORDER_UPDATED");
    }

    public static void orderDetailsChanged(String message) {
        notify("ORDER_DETAILS", AppEventType.ORDERS, "ORDER_DETAILS_CHANGED", message, "ORDER_DETAILS_UPDATED");
    }

    public static void inventoryChanged(String message) {
        notify("INVENTORY", AppEventType.INVENTORY, "INVENTORY_CHANGED", message, "STOCK_UPDATED");
    }

    public static void productsChanged(String message) {
        notify("PRODUCTS", AppEventType.PRODUCTS, "PRODUCTS_CHANGED", message, "PRODUCT_UPDATED");
    }

    public static void customersChanged(String message) {
        notify("CUSTOMERS", AppEventType.CUSTOMERS, "CUSTOMERS_CHANGED", message, "CUSTOMER_UPDATED");
    }

    public static void employeesChanged(String message) {
        notify("EMPLOYEES", AppEventType.EMPLOYEES, "EMPLOYEES_CHANGED", message, "EMPLOYEE_UPDATED");
    }

    public static void storesChanged(String message) {
        notify("STORES", AppEventType.STORE_INFO, "STORE_INFO_CHANGED", message, "STORE_UPDATED");
    }

    public static void accountSecurityChanged(String message) {
        notify("ACCOUNTS", AppEventType.ACCOUNT_SECURITY, "ACCOUNT_SECURITY_CHANGED", message, "ACCOUNT_UPDATED");
    }

    public static void systemConfigChanged(String message) {
        notify("SYSTEM_CONFIG", AppEventType.SYSTEM_CONFIG, "SYSTEM_CONFIG_CHANGED", message, "SYSTEM_CONFIG_UPDATED");
    }

    public static void dashboardChanged(String message) {
        notifyNoBump(AppEventType.DASHBOARD, "DASHBOARD_CHANGED", message, "DASHBOARD_UPDATED");
    }

    public static void statisticsChanged(String message) {
        notifyNoBump(AppEventType.STATISTICS, "STATISTICS_CHANGED", message, "STATISTICS_UPDATED");
    }

    public static void inventoryAlert(String message) {
        notifyNoBump(AppEventType.INVENTORY_ALERT, "INVENTORY_ALERT", message, "LOW_STOCK");
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
