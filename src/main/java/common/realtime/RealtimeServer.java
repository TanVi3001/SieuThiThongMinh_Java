package common.realtime;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public final class RealtimeServer extends WebSocketServer {

    public RealtimeServer(int port) {
        // 0.0.0.0: Lắng nghe trên mọi IP của máy (Bao gồm cả localhost và 10.0.23x.4x)
        // Đây là setting chuẩn nhất để chạy LAN!
        super(new InetSocketAddress("0.0.0.0", port));
    }

    public static void tryStart(int port) {
        try {
            RealtimeServer server = new RealtimeServer(port);
            server.start();
            System.out.println("[RT] WebSocket server started on 0.0.0.0:" + port);
        } catch (Exception e) {
            System.out.println("[RT] WebSocket server not started (maybe already running): " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[RT] client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[RT] client disconnected: " + conn.getRemoteSocketAddress() + " reason=" + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[RT] msg: " + message);
        broadcast(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("[RT] server error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        setConnectionLostTimeout(30);
        System.out.println("[RT] server onStart");
    }
}
