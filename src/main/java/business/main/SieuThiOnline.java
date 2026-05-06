package business.main;

import business.service.TokenCleanupService;
import business.sql.rbac.TokenSql;
import common.db.DatabaseConnection;
import common.report.ExcelExporter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

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

public class SieuThiOnline {

    public static void main(String[] args) {
        RealtimeServer.tryStart(9999);
        
        common.realtime.RealtimeClient.connect("ws://10.0.250.60");
        // UTF-8 output
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println("[LỖI] Hệ thống không hỗ trợ UTF-8: " + e.getMessage());
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("BẮT ĐẦU KIỂM THỬ TÍCH HỢP HỆ THỐNG");
        System.out.println("-------------------------------------------------------");

        // GIAI ĐOẠN 0: DB Connection
        System.out.println("[THÔNG BÁO] GĐ0 - Kiểm tra kết nối Oracle...");
        try (Connection con = DatabaseConnection.getConnection()) {
            if (con != null) {
                System.out.println("[HOÀN TẤT] Kết nối DB thành công.");
            } else {
                System.out.println("[THẤT BẠI] Kết nối DB trả về null.");
            }
        } catch (Exception e) {
            System.out.println("[CẢNH BÁO] Lỗi kết nối DB: " + e.getMessage());
        }

        // GĐ0.1: Cleanup token ngay khi app khởi động
        System.out.println("\n[THÔNG BÁO] GĐ0.1 - Dọn token hết hạn/revoked...");
        try {
            int deleted = TokenSql.getInstance().deleteExpiredTokens();
            System.out.println("[HOÀN TẤT] Startup token cleanup deleted rows = " + deleted);

            // bật cleanup định kỳ (ví dụ mỗi 15 phút)
            TokenCleanupService.start();
            System.out.println("[HOÀN TẤT] Đã bật TokenCleanupService định kỳ.");
        } catch (Exception e) {
            System.out.println("[CẢNH BÁO] Không dọn token được: " + e.getMessage());
            e.printStackTrace();
        }

        // GIAI ĐOẠN 1: Model + SQL Mapping
        System.out.println("\n[THÔNG BÁO] GĐ1 - Kiểm tra Model/SQL mapping...");
        try {
            System.out.println("--- 1.1 Supplier ---");
            List<Supplier> dsNhaCC = SuppliersSql.getInstance().selectAll();
            if (dsNhaCC == null || dsNhaCC.isEmpty()) {
                System.out.println("[!] DB chưa có dữ liệu Supplier.");
            } else {
                for (Supplier s : dsNhaCC) {
                    System.out.printf("ID: %-10s | Tên NCC: %-20s | SĐT: %s%n",
                            s.getSupplierId(), s.getSupplierName(), s.getPhoneNumber());
                }
            }

            System.out.println("\n--- 1.2 Store ---");
            List<Store> dsCuaHang = StoresSql.getInstance().selectAll();
            if (dsCuaHang != null) {
                for (Store st : dsCuaHang) {
                    System.out.println("Cửa hàng: " + st.getStoreName() + " - Địa chỉ: " + st.getAddress());
                }
            }

            System.out.println("\n--- 1.3 DeliveryManagement ---");
            List<DeliveryManagement> dsGiaoHang = DeliveryManagementSql.getInstance().selectAll();
            if (dsGiaoHang != null && !dsGiaoHang.isEmpty()) {
                DeliveryManagement dm = dsGiaoHang.get(0);
                System.out.println("Đơn giao thử nghiệm: " + dm.getOrderId()
                        + " | Trạng thái: " + dm.getDeliveryStatus());
            } else {
                System.out.println("[!] Chưa có dữ liệu DeliveryManagement.");
            }

        } catch (Exception e) {
            System.out.println("[CẢNH BÁO] Lỗi mapping Model/SQL: " + e.getMessage());
            e.printStackTrace();
        }

        // GIAI ĐOẠN 5: Export Excel
        System.out.println("\n[THÔNG BÁO] GĐ5 - Kiểm tra xuất báo cáo kho...");
        try {
            List<Inventory> dsTonKho = InventorySql.getInstance().selectAll();
            String filePath = "E:\\Inventory_Report_Vi.xlsx";

            if (dsTonKho == null || dsTonKho.isEmpty()) {
                System.out.println("[!] Không có dữ liệu tồn kho để xuất.");
            } else {
                ExcelExporter.exportInventory(dsTonKho, filePath);
                System.out.println("[HOÀN TẤT] Export Excel thành công: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("[CẢNH BÁO] Lỗi export Excel: " + e.getMessage());
            e.printStackTrace();
        }

        // GIAI ĐOẠN 6: Payment Transaction
        System.out.println("\n[THÔNG BÁO] GĐ6 - Kiểm tra giao dịch thanh toán...");
        String maHD = "HD_ST_1002";

        Order hd = new Order(
                maHD,
                "KH001",
                "EMP_01",
                new java.sql.Date(System.currentTimeMillis()),
                150000.0,
                "ĐÃ THANH TOÁN"
        );

        List<OrderDetail> gioHang = new ArrayList<>();
        gioHang.add(new OrderDetail(maHD, "PROD_MILK_01", 2, 20000.0));
        gioHang.add(new OrderDetail(maHD, "PROD_MILK_02", 5, 8000.0));

        try {
            boolean ok = PaymentService.thanhToan(hd, gioHang);
            if (ok) {
                System.out.println("[HOÀN TẤT] Giao dịch thành công: đã lưu hóa đơn + cập nhật tồn kho.");
            } else {
                System.out.println("[THẤT BẠI] Giao dịch thất bại: đã rollback.");
            }
        } catch (Exception e) {
            System.out.println("[NGHIÊM TRỌNG] Lỗi giao dịch: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n-------------------------------------------------------");
        System.out.println("KẾT THÚC QUY TRÌNH KIỂM THỬ TÍCH HỢP");
        System.out.println("-------------------------------------------------------");

        // UI
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(() -> new view.LoginView().setVisible(true));
    }
}
