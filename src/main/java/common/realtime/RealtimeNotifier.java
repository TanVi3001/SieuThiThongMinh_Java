package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;

public final class RealtimeNotifier {

    private RealtimeNotifier() {
    }

    public static void ordersChanged(String message) {
        publishLocal(AppEventType.ORDERS, message);
        sendRemote("ORDERS_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void inventoryChanged(String message) {
        publishLocal(AppEventType.INVENTORY, message);
        sendRemote("INVENTORY_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void productsChanged(String message) {
        publishLocal(AppEventType.PRODUCTS, message);
        sendRemote("PRODUCTS_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void customersChanged(String message) {
        publishLocal(AppEventType.CUSTOMERS, message);
        sendRemote("CUSTOMERS_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void employeesChanged(String message) {
        publishLocal(AppEventType.EMPLOYEES, message);
        sendRemote("EMPLOYEES_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void storesChanged(String message) {
        publishLocal(AppEventType.STORE_INFO, message);
        sendRemote("STORE_INFO_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
        sendRemote("STATISTICS_CHANGED:" + message);
    }

    public static void accountSecurityChanged(String message) {
        publishLocal(AppEventType.ACCOUNT_SECURITY, message);
        sendRemote("ACCOUNT_SECURITY_CHANGED:" + message);
        sendRemote("DASHBOARD_CHANGED:" + message);
    }

    public static void dashboardChanged(String message) {
        publishLocal(AppEventType.DASHBOARD, message);
        sendRemote("DASHBOARD_CHANGED:" + message);
    }

    public static void statisticsChanged(String message) {
        publishLocal(AppEventType.STATISTICS, message);
        sendRemote("STATISTICS_CHANGED:" + message);
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
}
