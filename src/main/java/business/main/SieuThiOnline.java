package business.main;

import business.service.TokenCleanupService;
import business.sql.rbac.TokenSql;
import common.db.DatabaseConnection;
import common.report.ExcelExporter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

// Model
import model.product.Supplier;
import model.product.Store;
import model.product.Inventory;
import model.order.DeliveryManagement;
import model.order.Order;
import model.order.OrderDetail;

// SQL
import business.sql.prod_inventory.SuppliersSql;
import business.sql.prod_inventory.StoresSql;
import business.sql.prod_inventory.InventorySql;
import business.sql.sales_order.DeliveryManagementSql;

// Service
import business.service.PaymentService;
import com.formdev.flatlaf.FlatLightLaf;
import common.realtime.RealtimeServer;
import common.realtime.RealtimeClient;
import view.LoginView;

public class SieuThiOnline {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        // 1. SETUP GIAO DIỆN TRƯỚC (ĐỂ HIỆN APP NGAY LẬP TỨC)
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("[LỖI] Không thể setup FlatLaf: " + e.getMessage());
        }

        // Mở màn hình Login ngay trên luồng Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });

        // 2. CHẠY TOÀN BỘ LOGIC KIỂM THỬ VÀ SERVICE TRONG LUỒNG NGẦM (CHỐNG ĐƠ UI)
        new Thread(() -> {
            runBackgroundSystems();
        }).start();
    }

    private static void runBackgroundSystems() {
        // UTF-8 output cho Console
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println("[LỖI] Hệ thống không hỗ trợ UTF-8");
        }

        System.out.println("\n--- [HỆ THỐNG] Đang khởi động các dịch vụ ngầm... ---");

        // KHỞI ĐỘNG REAL-TIME (Server trước, Client sau)
        String currentIp = common.utils.NetworkUtils.getLocalIPv4Address();
        System.out.println("Đang khởi động Server tại địa chỉ IP: " + currentIp);
        
        RealtimeServer.tryStart(8887);

        RealtimeClient.connect("ws://127.0.0.1:8887");


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
            System.err.println("[CẢNH BÁO] GĐ0.1 - Lỗi dọn dẹp token.");
        }

        System.out.println("\n-------------------------------------------------------");
        System.out.println("KẾT THÚC QUY TRÌNH KIỂM THỬ - HỆ THỐNG SẴN SÀNG");
        System.out.println("-------------------------------------------------------"); 
        
//         System.out.println(common.utils.PasswordUtils.hash("123456"));// --> Câu lệnh để lấy mã hash cho tài khoảng admin, 
//         //có mã này đem vào mật khẩu trong Bảng Account ->Commit sẽ đăng nhập được -> sau khi chạy xong phải log dòng này lại

    }
}
//Tách 2 cái này ra, 1 cái là nội dung tạo bảng, 1 cái là nội dung insert dữ liệu đầu vào trừ(trừ admin, tài khoản)

