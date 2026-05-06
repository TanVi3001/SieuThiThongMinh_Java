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

    // ÉP CỨNG IP LAN LÚC DEMO CHO AN TOÀN
    private static final String DEFAULT_LAN_WS_URL = "ws://192.168.88.210";

    private RealtimeClient() {
    }

    public static void connect(String wsUrl) {
        try {
            // Chốt chặn: Nếu wsUrl truyền vào bị rỗng hoặc đang là localhost, ép nó về IP LAN luôn
            if (wsUrl == null || wsUrl.isEmpty() || wsUrl.contains("localhost") || wsUrl.contains("10.0.250.60")) {
                wsUrl = DEFAULT_LAN_WS_URL;
            }

            serverUri = URI.create(wsUrl);

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[RT] connected to " + serverUri);
                }

                @Override
                public void onMessage(String message) {
                    if ("PRODUCTS_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.PRODUCTS, "realtime"));
                    } else if ("INVENTORY_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.INVENTORY, "realtime"));
                    } else if ("SYSTEM_CONFIG_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "realtime"));
                    } else if ("ACCOUNT_SECURITY_CHANGED".equalsIgnoreCase(message)) {
                        EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "realtime"));
                    } // THÊM 2 DÒNG NÀY VÀO NÈ VĨ:
                    else if ("CUSTOMERS_CHANGED".equalsIgnoreCase(message)) {
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
                    // Dùng lại URI đã được chốt ở hàm connect
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
            }
        } catch (Exception e) {
            System.out.println("[RT] send failed: " + e.getMessage());
        }
    }
}
