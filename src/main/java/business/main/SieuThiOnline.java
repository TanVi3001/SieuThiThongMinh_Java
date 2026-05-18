package business.main;

import business.service.TokenCleanupService;
import business.sql.rbac.TokenSql;
import common.db.DatabaseConnection;
import common.realtime.RealtimeClient;
import common.realtime.RealtimeServer;
import com.formdev.flatlaf.FlatLightLaf;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import javax.swing.SwingUtilities;
import model.account.Account;
import view.LoginView;

public class SieuThiOnline {

    public static void main(String[] args) {
        setupUtf8Console();
        setupShutdownHook();
        setupLookAndFeel();
        openLoginView();
        startBackgroundSystems();
    }

    private static void setupUtf8Console() {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("[LỖI] Hệ thống không hỗ trợ UTF-8: " + e.getMessage());
        }
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Account currentUser = business.service.SessionManager.getCurrentUser();
                String sessionId = business.service.SessionManager.getCurrentSessionId();

                if (currentUser == null
                        || currentUser.getAccountId() == null
                        || sessionId == null) {
                    return;
                }

                if (business.service.HeartbeatService.markLogoutOnce()) {
                    business.service.HeartbeatService.stop();

                    business.service.AccountService.onLogoutOrCloseApp(
                            currentUser.getAccountId(),
                            sessionId
                    );
                }

            } catch (Exception ex) {
                System.err.println("[ShutdownHook] Không thể cập nhật session: " + ex.getMessage());
            }
        }, "shutdown-hook-session-cleanup"));
    }

    private static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("[LỖI] Không thể setup FlatLaf: " + e.getMessage());
        }
    }

    private static void openLoginView() {
        SwingUtilities.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }

    private static void startBackgroundSystems() {
        Thread backgroundThread = new Thread(
                SieuThiOnline::runBackgroundSystems,
                "background-system-thread"
        );

        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private static void runBackgroundSystems() {
        System.out.println();
        System.out.println("=======================================================");
        System.out.println("[HỆ THỐNG] Đang khởi động các dịch vụ nền...");
        System.out.println("=======================================================");

        startRealtimeSystem();
        runIntegrationChecks();

        System.out.println();
        System.out.println("=======================================================");
        System.out.println("[HỆ THỐNG] Hoàn tất khởi động dịch vụ nền.");
        System.out.println("=======================================================");
    }

    private static void startRealtimeSystem() {
        String currentIp = common.utils.NetworkUtils.getLocalIPv4Address();

        System.out.println("[REALTIME] IP máy hiện tại: " + currentIp);

        try {
            RealtimeServer.tryStart(8887);
            RealtimeClient.connect("ws://127.0.0.1:8887");

            System.out.println("[HOÀN TẤT] Real-time Server/Client đã khởi động.");
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] Không thể khởi động Real-time: " + e.getMessage());
        }
    }

    private static void runIntegrationChecks() {
        System.out.println("-------------------------------------------------------");
        System.out.println("BẮT ĐẦU KIỂM TRA TÍCH HỢP HỆ THỐNG");
        System.out.println("-------------------------------------------------------");

        checkDatabaseConnection();
        cleanupExpiredTokens();
        cleanupDeadSessions();

        System.out.println("-------------------------------------------------------");
        System.out.println("KẾT THÚC KIỂM TRA TÍCH HỢP - HỆ THỐNG SẴN SÀNG");
        System.out.println("-------------------------------------------------------");
    }

    private static void checkDatabaseConnection() {
        try (Connection con = DatabaseConnection.getConnection()) {
            if (con != null && !con.isClosed()) {
                System.out.println("[HOÀN TẤT] GĐ0 - Kết nối Database thành công.");
                System.out.println("[DB] URL  : " + DatabaseConnection.getCurrentJdbcUrlForLog());
                System.out.println("[DB] User : " + DatabaseConnection.getCurrentUsernameForLog());
            }
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0 - Lỗi kết nối DB: " + e.getMessage());
        }
    }

    private static void cleanupExpiredTokens() {
        try {
            int deleted = TokenSql.getInstance().deleteExpiredTokens();

            System.out.println("[HOÀN TẤT] GĐ0.1 - Đã dọn " + deleted + " token hết hạn.");

            TokenCleanupService.start();
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0.1 - Lỗi dọn dẹp token: " + e.getMessage());
        }
    }

    private static void cleanupDeadSessions() {
        try {
            business.service.AccountService.cleanupDeadSessions();

            System.out.println("[HOÀN TẤT] GĐ0.2 - Đã kiểm tra session treo.");
        } catch (Exception e) {
            System.err.println("[CẢNH BÁO] GĐ0.2 - Lỗi cleanup session: " + e.getMessage());
        }
    }
}
