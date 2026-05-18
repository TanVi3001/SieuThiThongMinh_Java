package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RealtimeClient {

    private static volatile WebSocketClient client;
    private static volatile URI serverUri;
    private static volatile boolean reconnectScheduled = false;

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
        // 1. Xử lý logic URL/IP TRƯỚC khi tạo URI

        // Ưu tiên dùng IP tĩnh để tránh lỗi phân giải IPv6 của Windows/Localhost
        if (wsUrl == null || wsUrl.isEmpty() || wsUrl.contains("localhost")) {
            // Có thể đổi thành 127.0.0.1 hoặc IP máy chủ cụ thể của ông
            wsUrl = "ws://127.0.0.1:8887";

        }

        try {
            serverUri = URI.create(wsUrl);

            // 2. Quản lý kết nối cũ
            if (client != null) {
                if (client.isOpen()) {
                    System.out.println("[RT] Đã online, bỏ qua kết nối mới.");
                    return;
                }
                // Dọn dẹp client cũ
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }

            System.out.println("[RT] Đang thử kết nối tới: " + serverUri);

            // 3. Khởi tạo Client mới
            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    reconnectScheduled = false;
                    System.out.println("[RT] CONNECTED: " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[RT] MSG RECEIVED: " + message);

                    AppEventType type = mapMessageToType(message);

                    if (type != null) {
                        EventBus.publish(new AppDataChangedEvent(type, message));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code != 1000) { // 1000 là đóng chủ động (Normal Closure)
                        System.out.println("[RT] DISCONNECTED: " + reason + " (code=" + code + ")");
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[RT] ERROR: " + ex.getMessage());
                }
            };

            client.connect();

        } catch (Exception e) {
            System.err.println("[RT] CONNECT CRITICAL FAILED: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private static void scheduleReconnect() {
        if (serverUri == null || reconnectScheduled) {
            return;
        }

        reconnectScheduled = true;
        System.out.println("[RT] Sẽ thử kết nối lại sau 3 giây...");

        RECONNECT_SCHEDULER.schedule(() -> {
            reconnectScheduled = false;
            if (!isOnline()) {
                connect(serverUri.toString());
            }
        }, 3, TimeUnit.SECONDS);
    }

    public static void send(String message) {
        try {
            if (isOnline()) {
                client.send(message);
                System.out.println("[RT] SENT: " + message);
            } else {
                System.err.println("[RT] SEND FAILED: Client offline. Msg: " + message);
            }
        } catch (Exception e) {
            System.err.println("[RT] SEND ERROR: " + e.getMessage());
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
}
