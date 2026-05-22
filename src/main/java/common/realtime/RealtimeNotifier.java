package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.sync.SyncVersionDao;

/**
 * Centralized realtime mapping helper.
 *
 * Rule: call these methods only AFTER database changes are committed successfully.
 * Each method publishes local EventBus events for the current app and sends websocket
 * messages for other running app instances. Dashboard and statistics companion events
 * are included for summary panels such as AdminSystemPanel.
 */
public final class RealtimeNotifier {

    private RealtimeNotifier() {
    }

    public static void ordersChanged(String message) {
        bump("ORDERS");
        publishLocal(AppEventType.ORDERS, normalize(message, "ORDERS_CHANGED"));
        sendRemote("ORDERS_CHANGED:" + normalize(message, "ORDER_UPDATED"));
        dashboardChanged("FROM_ORDERS:" + normalize(message, "ORDER_UPDATED"));
        statisticsChanged("FROM_ORDERS:" + normalize(message, "ORDER_UPDATED"));
    }

    public static void orderDetailsChanged(String message) {
        bump("ORDER_DETAILS");
        publishLocal(AppEventType.ORDERS, normalize(message, "ORDER_DETAILS_CHANGED"));
        sendRemote("ORDER_DETAILS_CHANGED:" + normalize(message, "ORDER_DETAILS_UPDATED"));
        ordersChanged("FROM_ORDER_DETAILS:" + normalize(message, "ORDER_DETAILS_UPDATED"));
    }

    public static void inventoryChanged(String message) {
        bump("INVENTORY");
        publishLocal(AppEventType.INVENTORY, normalize(message, "INVENTORY_CHANGED"));
        sendRemote("INVENTORY_CHANGED:" + normalize(message, "STOCK_UPDATED"));
        dashboardChanged("FROM_INVENTORY:" + normalize(message, "STOCK_UPDATED"));
        statisticsChanged("FROM_INVENTORY:" + normalize(message, "STOCK_UPDATED"));
    }

    public static void productsChanged(String message) {
        bump("PRODUCTS");
        publishLocal(AppEventType.PRODUCTS, normalize(message, "PRODUCTS_CHANGED"));
        sendRemote("PRODUCTS_CHANGED:" + normalize(message, "PRODUCT_UPDATED"));
        dashboardChanged("FROM_PRODUCTS:" + normalize(message, "PRODUCT_UPDATED"));
        statisticsChanged("FROM_PRODUCTS:" + normalize(message, "PRODUCT_UPDATED"));
    }

    public static void customersChanged(String message) {
        bump("CUSTOMERS");
        publishLocal(AppEventType.CUSTOMERS, normalize(message, "CUSTOMERS_CHANGED"));
        sendRemote("CUSTOMERS_CHANGED:" + normalize(message, "CUSTOMER_UPDATED"));
        dashboardChanged("FROM_CUSTOMERS:" + normalize(message, "CUSTOMER_UPDATED"));
        statisticsChanged("FROM_CUSTOMERS:" + normalize(message, "CUSTOMER_UPDATED"));
    }

    public static void employeesChanged(String message) {
        bump("EMPLOYEES");
        publishLocal(AppEventType.EMPLOYEES, normalize(message, "EMPLOYEES_CHANGED"));
        sendRemote("EMPLOYEES_CHANGED:" + normalize(message, "EMPLOYEE_UPDATED"));
        dashboardChanged("FROM_EMPLOYEES:" + normalize(message, "EMPLOYEE_UPDATED"));
        statisticsChanged("FROM_EMPLOYEES:" + normalize(message, "EMPLOYEE_UPDATED"));
    }

    public static void storesChanged(String message) {
        bump("STORES");
        publishLocal(AppEventType.STORE_INFO, normalize(message, "STORE_INFO_CHANGED"));
        sendRemote("STORE_INFO_CHANGED:" + normalize(message, "STORE_UPDATED"));
        dashboardChanged("FROM_STORES:" + normalize(message, "STORE_UPDATED"));
        statisticsChanged("FROM_STORES:" + normalize(message, "STORE_UPDATED"));
    }

    public static void accountSecurityChanged(String message) {
        bump("ACCOUNTS");
        publishLocal(AppEventType.ACCOUNT_SECURITY, normalize(message, "ACCOUNT_SECURITY_CHANGED"));
        sendRemote("ACCOUNT_SECURITY_CHANGED:" + normalize(message, "ACCOUNT_UPDATED"));
        dashboardChanged("FROM_ACCOUNT_SECURITY:" + normalize(message, "ACCOUNT_UPDATED"));
    }

    public static void systemConfigChanged(String message) {
        bump("SYSTEM_CONFIG");
        publishLocal(AppEventType.SYSTEM_CONFIG, normalize(message, "SYSTEM_CONFIG_CHANGED"));
        sendRemote("SYSTEM_CONFIG_CHANGED:" + normalize(message, "SYSTEM_CONFIG_UPDATED"));
        dashboardChanged("FROM_SYSTEM_CONFIG:" + normalize(message, "SYSTEM_CONFIG_UPDATED"));
        statisticsChanged("FROM_SYSTEM_CONFIG:" + normalize(message, "SYSTEM_CONFIG_UPDATED"));
    }

    public static void dashboardChanged(String message) {
        publishLocal(AppEventType.DASHBOARD, normalize(message, "DASHBOARD_CHANGED"));
        sendRemote("DASHBOARD_CHANGED:" + normalize(message, "DASHBOARD_UPDATED"));
    }

    public static void statisticsChanged(String message) {
        publishLocal(AppEventType.STATISTICS, normalize(message, "STATISTICS_CHANGED"));
        sendRemote("STATISTICS_CHANGED:" + normalize(message, "STATISTICS_UPDATED"));
    }

    public static void inventoryAlert(String message) {
        publishLocal(AppEventType.INVENTORY_ALERT, normalize(message, "INVENTORY_ALERT"));
        sendRemote("INVENTORY_ALERT:" + normalize(message, "LOW_STOCK"));
        dashboardChanged("FROM_INVENTORY_ALERT:" + normalize(message, "LOW_STOCK"));
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
