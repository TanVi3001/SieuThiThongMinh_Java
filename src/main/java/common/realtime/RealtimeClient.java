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

    private static final ScheduledExecutorService RECONNECT_SCHEDULER
            = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RealtimeClient-Reconnect");
                t.setDaemon(true);
                return t;
            });

    private static volatile boolean reconnectScheduled = false;

    private RealtimeClient() {
    }

    public static boolean isOnline() {
        return client != null && client.isOpen();
    }

    public static void connect(String wsUrl) {
        try {
            // Ưu tiên dùng localhost (127.0.0.1) và port 8887 cho WebSocket nội bộ
            if (wsUrl == null || wsUrl.isEmpty()) {
                wsUrl = "ws://127.0.0.1:8887";
            }

            serverUri = URI.create(wsUrl);
            System.out.println("[RT] Đang thử kết nối tới: " + serverUri);

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    reconnectScheduled = false;
                    System.out.println("[RT] CONNECTED: " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[RT] MSG: " + message);
                    AppEventType type = mapMessageToType(message);
                    if (type != null) {
                        EventBus.publish(new AppDataChangedEvent(type, "realtime"));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[RT] DISCONNECTED: " + reason + " (code=" + code + ")");
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[RT] ERROR: " + ex);
                    scheduleReconnect();
                }
            };

            client.connect();
        } catch (Exception e) {
            System.err.println("[RT] CONNECT FAILED: " + e);
            scheduleReconnect();
        }
    }

    private static AppEventType mapMessageToType(String message) {
        if (message == null) {
            return null;
        }
        if ("PRODUCTS_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.PRODUCTS;
        }
        if ("INVENTORY_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.INVENTORY;
        }
        if ("SYSTEM_CONFIG_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.SYSTEM_CONFIG;
        }
        if ("ACCOUNT_SECURITY_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.ACCOUNT_SECURITY;
        }
        if ("CUSTOMERS_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.CUSTOMERS;
        }
        if ("EMPLOYEES_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.EMPLOYEES;
        }
        if ("ORDERS_CHANGED".equalsIgnoreCase(message)) {
            return AppEventType.ORDERS;
        }
        return null;
    }

    private static void scheduleReconnect() {
        if (serverUri == null || reconnectScheduled) {
            return;
        }

        reconnectScheduled = true;
        RECONNECT_SCHEDULER.schedule(() -> {
            try {
                if (!isOnline()) {
                    System.out.println("[RT] Reconnecting to " + serverUri + " ...");
                    connect(serverUri.toString());
                } else {
                    reconnectScheduled = false;
                }
            } catch (Exception ignored) {
            }
        }, 3, TimeUnit.SECONDS);
    }

    public static void send(String message) {
        try {
            if (isOnline()) {
                client.send(message);
                System.out.println("[RT] SENT: " + message);
            } else {
                System.err.println("[RT] SEND FAILED (offline): " + message);
            }
        } catch (Exception e) {
            System.err.println("[RT] SEND ERROR: " + e.getMessage());
        }
    }
}
