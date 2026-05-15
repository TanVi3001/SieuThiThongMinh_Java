package business.main;

import business.service.TokenCleanupService;
import business.sql.rbac.TokenSql;
import common.db.DatabaseConnection;
import java.sql.Connection;
import javax.swing.SwingUtilities;
import business.service.AccountService;

import com.formdev.flatlaf.FlatLightLaf;
import common.realtime.RealtimeServer;
import common.realtime.RealtimeClient;
import view.LoginView;

public class SieuThiOnline {

    public static void main(String[] args) {

        // =========================================================
        // SHUTDOWN HOOK
        // Trường hợp app bị đóng bằng System.exit hoặc tắt cửa sổ,
        // hệ thống sẽ cố gắng trừ active_sessions trước khi thoát.
        // =========================================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                model.account.Account currentUser
                        = business.service.SessionManager.getCurrentUser();

                String sessionId
                        = business.service.SessionManager.getCurrentSessionId();

                if (currentUser != null
                        && currentUser.getAccountId() != null
                        && sessionId != null) {

                    if (business.service.HeartbeatService.markLogoutOnce()) {
                        business.service.HeartbeatService.stop();

                        business.service.AccountService.onLogoutOrCloseApp(
                                currentUser.getAccountId(),
                                sessionId
                        );
                    }
                }

            } catch (Exception ex) {
                System.err.println("[ShutdownHook] Không thể cập nhật session: " + ex.getMessage());
            }
        }));
        // =========================================================
        // SETUP GIAO DIỆN
        // =========================================================
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("[LỖI] Không thể setup FlatLaf: " + e.getMessage());
        }

        // Mở màn hình Login trên EDT để tránh đơ UI
        SwingUtilities.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });

        // Chạy service nền
        new Thread(SieuThiOnline::runBackgroundSystems, "background-system-thread").start();
    }

    private static void runBackgroundSystems() {
        // UTF-8 output cho Console
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println("[LỖI] Hệ thống không hỗ trợ UTF-8");
        }

        System.out.println("\n--- [HỆ THỐNG] Đang khởi động các dịch vụ ngầm... ---");

        // KHỞI ĐỘNG REAL-TIME
        String currentIp = common.utils.NetworkUtils.getLocalIPv4Address();
        System.out.println("Đang khởi động Server tại địa chỉ IP: " + currentIp);

        try {
            RealtimeServer.tryStart(8887);
            RealtimeClient.connect("ws://127.0.0.1:8887");
            System.out.println("[HOÀN TẤT] Real-time Server/Client đã khởi động.");
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] Không thể khởi động Real-time: " + e.getMessage());
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("BẮT ĐẦU QUY TRÌNH KIỂM THỬ TÍCH HỢP HỆ THỐNG");
        System.out.println("-------------------------------------------------------");

        // GĐ0: DB Connection
        try (Connection con = DatabaseConnection.getConnection()) {
            if (con != null) {
                System.out.println("[HOÀN TẤT] GĐ0 - Kết nối Database thành công.");
            }
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0 - Lỗi kết nối DB: " + e.getMessage());
        }

        // GĐ0.1: Dọn dẹp Token
        try {
            int deleted = TokenSql.getInstance().deleteExpiredTokens();
            System.out.println("[HOÀN TẤT] GĐ0.1 - Đã dọn " + deleted + " token hết hạn.");
            TokenCleanupService.start();
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0.1 - Lỗi dọn dẹp token: " + e.getMessage());
        }

        // GĐ0.2: Dọn session treo do crash/mất điện
        try {
            business.service.AccountService.cleanupDeadSessions();
            System.out.println("[HOÀN TẤT] GĐ0.2 - Đã kiểm tra session treo.");
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0.2 - Lỗi cleanup session: " + e.getMessage());
        }

        System.out.println("\n-------------------------------------------------------");
        System.out.println("KẾT THÚC QUY TRÌNH KIỂM THỬ - HỆ THỐNG SẴN SÀNG");
        System.out.println("-------------------------------------------------------");

//         System.out.println(common.utils.PasswordUtils.hash("123456"));// --> Câu lệnh để lấy mã hash cho tài khoảng admin, 
//         //có mã này đem vào mật khẩu trong Bảng Account ->Commit sẽ đăng nhập được -> sau khi chạy xong phải log dòng này lại
    }
}
