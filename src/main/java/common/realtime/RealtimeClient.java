package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class RealtimeClient {

    private static volatile WebSocketClient client;
    private static volatile URI serverUri;

    private RealtimeClient() {
    }

    public static void connect(String wsUrl) {
        try {
            // FIX: Dùng cứng 127.0.0.1 để Windows không bị nhầm lẫn IPv6
            if (wsUrl == null || wsUrl.isEmpty() || wsUrl.contains("localhost")) {
                wsUrl = "ws://10.0.250.60:9999";
            }

            serverUri = URI.create(wsUrl);

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[RT] connected to " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[RT] Client nhận được lệnh: " + message);
                    if ("PRODUCTS_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.PRODUCTS, "realtime"));
                    } else if ("INVENTORY_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.INVENTORY, "realtime"));
                    } else if ("SYSTEM_CONFIG_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "realtime"));
                    } else if ("ACCOUNT_SECURITY_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "realtime"));
                    } else if ("CUSTOMERS_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.CUSTOMERS, "realtime"));
                    } else if ("EMPLOYEES_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.EMPLOYEES, "realtime"));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[RT] disconnected: " + reason + " (code=" + code + ")");
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.out.println("[RT] client error: " + ex.getMessage());
                }
            };

            client.connect();
        } catch (Exception e) {
            System.out.println("[RT] connect failed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private static void scheduleReconnect() {
        if (serverUri == null) {
            return;
        }
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                if (client == null || !client.isOpen()) {
                    System.out.println("[RT] reconnecting to " + serverUri);
                    connect(serverUri.toString());
                }
            } catch (Exception ignored) {
            }
        }, 2, TimeUnit.SECONDS);
    }

    public static void send(String message) {
        try {
            if (client != null && client.isOpen()) {
                client.send(message);
                System.out.println("[RT] Đã gửi thành công lệnh lên Server: " + message);
            } else {
                System.err.println("[RT] Lỗi: Client chưa kết nối, không thể gửi WebSocket!");
            }
        } catch (Exception e) {
            System.out.println("[RT] send failed: " + e.getMessage());
        }
    }
}