package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Method;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public final class RealtimeClient {

    private static volatile WebSocketClient client;
    private static volatile URI serverUri;
    private static volatile boolean reconnectScheduled = false;
    private static volatile long lastAdminPanelRefreshAt = 0L;

    private static final boolean DEBUG = Boolean.getBoolean("app.debug.realtime");
    private static final long ADMIN_REFRESH_THROTTLE_MS = 300L;
    private static final long RECONNECT_DELAY_SECONDS = 1L;

    private static final ScheduledExecutorService RECONNECT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RealtimeClient-Reconnect");
        t.setDaemon(true);
        return t;
    });

    private RealtimeClient() {
    }

    public static boolean isOnline() {
        return client != null && client.isOpen();
    }

    public static void connect(String wsUrl) {
        if (wsUrl == null || wsUrl.isEmpty() || wsUrl.contains("localhost")) {
            wsUrl = "ws://127.0.0.1:8887";
        }

        try {
            serverUri = URI.create(wsUrl);

            if (client != null) {
                if (client.isOpen()) {
                    debug("Already online, skip reconnect");
                    return;
                }
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }

            debug("Connecting to " + serverUri);

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    reconnectScheduled = false;
                    debug("Connected: " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    debug("Message received: " + message);
                    dispatchRealtimeMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code != 1000) {
                        debug("Disconnected: code=" + code + ", reason=" + reason);
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    debug("Error: " + ex.getMessage());
                }
            };

            client.connect();

        } catch (Exception e) {
            debug("Connect failed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private static void scheduleReconnect() {
        if (serverUri == null || reconnectScheduled) {
            return;
        }

        reconnectScheduled = true;
        debug("Reconnect scheduled after " + RECONNECT_DELAY_SECONDS + "s");

        RECONNECT_SCHEDULER.schedule(() -> {
            reconnectScheduled = false;
            if (!isOnline()) {
                connect(serverUri.toString());
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public static void send(String message) {
        dispatchRealtimeMessage(message);
        sendRemoteOnly(message);
    }

    public static void sendRemoteOnly(String message) {
        try {
            if (isOnline()) {
                client.send(message);
                debug("Sent: " + message);
            } else {
                debug("Send skipped because client is offline: " + message);
            }
        } catch (Exception e) {
            debug("Send error: " + e.getMessage());
        }
    }

    private static void dispatchRealtimeMessage(String message) {
        AppEventType type = mapMessageToType(message);

        if (type != null) {
            publishRealtimeEvent(type, message);
            refreshAdminSystemPanels(type, message);
        }
    }

    private static void publishRealtimeEvent(AppEventType type, String message) {
        try {
            EventBus.publish(new AppDataChangedEvent(type, message));
        } catch (Exception ex) {
            debug("EventBus error: " + ex.getMessage());
        }
    }

    private static boolean affectsDashboard(AppEventType type) {
        return type == AppEventType.ORDERS
                || type == AppEventType.INVENTORY
                || type == AppEventType.INVENTORY_ALERT
                || type == AppEventType.PRODUCTS
                || type == AppEventType.CUSTOMERS
                || type == AppEventType.EMPLOYEES
                || type == AppEventType.STORE_INFO
                || type == AppEventType.ACCOUNT_SECURITY
                || type == AppEventType.SYSTEM_CONFIG
                || type == AppEventType.DASHBOARD
                || type == AppEventType.STATISTICS;
    }

    private static void refreshAdminSystemPanels(AppEventType type, String message) {
        if (!affectsDashboard(type)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAdminPanelRefreshAt < ADMIN_REFRESH_THROTTLE_MS) {
            return;
        }
        lastAdminPanelRefreshAt = now;

        SwingUtilities.invokeLater(() -> {
            for (Window window : Window.getWindows()) {
                refreshAdminSystemPanelIn(window);
            }
        });
    }

    private static void refreshAdminSystemPanelIn(Component component) {
        if (component == null) {
            return;
        }

        if ("view.AdminSystemPanel".equals(component.getClass().getName())) {
            try {
                Method reloadAll = component.getClass().getDeclaredMethod("reloadAll");
                reloadAll.setAccessible(true);
                reloadAll.invoke(component);
                debug("AdminSystemPanel reloaded by realtime event");
            } catch (Exception ex) {
                debug("AdminSystemPanel reload failed: " + ex.getMessage());
            }
            return;
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshAdminSystemPanelIn(child);
            }
        }
    }

    private static AppEventType mapMessageToType(String message) {
        if (message == null || message.trim().isEmpty()) {
            return AppEventType.UNKNOWN;
        }

        String msg = message.trim().toUpperCase();

        if (msg.startsWith("INVENTORY_ALERT")) {
            return AppEventType.INVENTORY_ALERT;
        }

        if (msg.contains("PROMOTION_CHANGED") || msg.contains("PROMOTIONS_CHANGED") || msg.contains("PROMO_UPDATED")) {
            return AppEventType.STORE_INFO;
        }

        if (msg.contains("PRODUCTS_CHANGED") || msg.contains("PRODUCT_CHANGED")) {
            return AppEventType.PRODUCTS;
        }

        if (msg.contains("INVENTORY_CHANGED") || msg.contains("STOCK_CHANGED")) {
            return AppEventType.INVENTORY;
        }

        if (msg.contains("ORDERS_CHANGED") || msg.contains("ORDER_CHANGED")) {
            return AppEventType.ORDERS;
        }

        if (msg.contains("CUSTOMERS_CHANGED") || msg.contains("CUSTOMER_CHANGED")) {
            return AppEventType.CUSTOMERS;
        }

        if (msg.contains("EMPLOYEES_CHANGED") || msg.contains("EMPLOYEE_CHANGED")) {
            return AppEventType.EMPLOYEES;
        }

        if (msg.contains("ACCOUNT_SECURITY_CHANGED") || msg.contains("ACCOUNT_CHANGED")) {
            return AppEventType.ACCOUNT_SECURITY;
        }

        if (msg.contains("SYSTEM_CONFIG_CHANGED")) {
            return AppEventType.SYSTEM_CONFIG;
        }

        if (msg.contains("STORE_INFO_CHANGED") || msg.contains("STORE_CHANGED")) {
            return AppEventType.STORE_INFO;
        }

        if (msg.contains("DASHBOARD_CHANGED")) {
            return AppEventType.DASHBOARD;
        }

        if (msg.contains("STATISTICS_CHANGED") || msg.contains("REPORT_CHANGED")) {
            return AppEventType.STATISTICS;
        }

        return AppEventType.UNKNOWN;
    }

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println("[RT] " + message);
        }
    }
}
