package common.realtime;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

public class RealtimeServer extends WebSocketServer {

    private static RealtimeServer instance;
    private static final boolean DEBUG = Boolean.getBoolean("app.debug.realtime");

    public RealtimeServer(InetSocketAddress address) {
        super(address);
    }

    public static void tryStart(int port) {
        if (instance == null) {
            try {
                instance = new RealtimeServer(new InetSocketAddress("0.0.0.0", port));
                instance.start();
                debug("WebSocket server started on port " + port);
            } catch (Exception e) {
                System.err.println("[RT] Server start failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        debug("Client connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        debug("Client disconnected: code=" + code);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        broadcast(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        debug("Network error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        debug("Server ready");
    }

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println("[RT] " + message);
        }
    }
}
