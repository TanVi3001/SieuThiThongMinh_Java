package common.realtime;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

public class RealtimeServer extends WebSocketServer {

    private static RealtimeServer instance;

    public RealtimeServer(InetSocketAddress address) {
        super(address);
    }

    public static void tryStart(int port) {
        if (instance == null) {
            try {
                instance = new RealtimeServer(new InetSocketAddress("0.0.0.0", port));
                instance.start();
                System.out.println("[RT] WebSocket Server đã khởi động trên port " + port);
            } catch (Exception e) {
                System.err.println("Lỗi khởi động Server: " + e.getMessage());
            }
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[SERVER] Một máy vừa kết nối!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[SERVER] Một máy đã ngắt kết nối.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[SERVER] Nhận được tin nhắn: " + message);
        // =======================================================
        // BƯỚC QUAN TRỌNG NHẤT: Phát loa cho tất cả các máy khác!
        // =======================================================
        broadcast(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[SERVER] Lỗi mạng: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[SERVER] Đã sẵn sàng truyền tín hiệu!");
    }
}
