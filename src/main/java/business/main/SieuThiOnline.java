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
import common.realtime.RealtimeServer;
import common.realtime.RealtimeClient;
import view.LoginView;

public class SieuThiOnline {

    public static void main(String[] args) {
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
        RealtimeServer.tryStart(8887);
        RealtimeClient.connect("ws://10.0.214.135:8887");

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

//        // GĐ1: Model + SQL Mapping
//        try {
//            System.out.println("\n--- GĐ1: Kiểm tra Model/SQL Mapping ---");
//            List<Supplier> dsNhaCC = SuppliersSql.getInstance().selectAll();
//            if (dsNhaCC != null && !dsNhaCC.isEmpty()) {
//                System.out.println("-> Lấy được " + dsNhaCC.size() + " nhà cung cấp.");
//            }
//
//            List<Store> dsCuaHang = StoresSql.getInstance().selectAll();
//            if (dsCuaHang != null) {
//                System.out.println("-> Lấy được " + dsCuaHang.size() + " cửa hàng.");
//            }
//        } catch (Exception e) {
//            System.err.println("[!] Lỗi Mapping dữ liệu.");
//        }
//
//        // GĐ5: Xuất báo cáo Excel (Việc nặng)
//        try {
//            List<Inventory> dsTonKho = InventorySql.getInstance().selectAll();
//            if (dsTonKho != null && !dsTonKho.isEmpty()) {
//                String filePath = "E:\\Inventory_Report_Vi.xlsx";
//                ExcelExporter.exportInventory(dsTonKho, filePath);
//                System.out.println("\n[HOÀN TẤT] GĐ5 - Đã xuất báo cáo Excel tại: " + filePath);
//            }
//        } catch (Exception e) {
//            System.err.println("[!] GĐ5 - Lỗi xuất Excel: " + e.getMessage());
//        }
//
//        // GĐ6: Transaction Thanh toán (Dễ kẹt DB nhất)
//        try {
//            System.out.println("\n--- GĐ6: Kiểm tra giao dịch thanh toán ---");
//            String maHD = "HD_ST_1002";
//            Order hd = new Order(maHD, "KH001", "EMP_01", new java.sql.Date(System.currentTimeMillis()), 150000.0, "ĐÃ THANH TOÁN");
//            List<OrderDetail> gioHang = new ArrayList<>();
//            gioHang.add(new OrderDetail(maHD, "PROD_MILK_01", 2, 20000.0));
//            gioHang.add(new OrderDetail(maHD, "PROD_MILK_02", 5, 8000.0));
//
//            boolean ok = PaymentService.thanhToan(hd, gioHang);
//            System.out.println(ok ? "[HOÀN TẤT] Giao dịch thành công." : "[THẤT BẠI] Giao dịch thất bại.");
//        } catch (Exception e) {
//            System.err.println("[NGHIÊM TRỌNG] GĐ6 - Lỗi logic thanh toán.");
//        }

        System.out.println("\n-------------------------------------------------------");
        System.out.println("KẾT THÚC QUY TRÌNH KIỂM THỬ - HỆ THỐNG SẴN SÀNG");
        System.out.println("-------------------------------------------------------");
    }
}
