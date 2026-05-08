package business.service;

import common.db.DatabaseConnection;
import business.sql.prod_inventory.ProductUnitsSql;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.OrdersSql;      // DAO cho hóa đơn
import business.sql.sales_order.OrderDetailsSql; // DAO cho chi tiết
import model.order.Order;
import model.order.OrderDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PaymentService {

    public static boolean thanhToan(Order hoaDon, List<OrderDetail> dsChiTiet) {
        java.sql.Connection con = null;
        try {
            con = common.db.DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Bật chế độ Transaction an toàn

            // =========================================================
            // BƯỚC 1: VALIDATE TỒN KHO BẰNG CỘT `STOCK_QUANTITY`
            // =========================================================
            for (OrderDetail ct : dsChiTiet) {
                // SỬ DỤNG ĐÚNG CỘT `stock_quantity` NHƯ TRONG HÌNH 3
                String checkStockSql = "SELECT stock_quantity FROM PRODUCTS WHERE product_id = ?";
                try (java.sql.PreparedStatement psCheck = con.prepareStatement(checkStockSql)) {
                    psCheck.setString(1, ct.getProductId());
                    try (java.sql.ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            int currentStock = rsCheck.getInt("stock_quantity");
                            if (currentStock < ct.getQuantity()) {
                                javax.swing.JOptionPane.showMessageDialog(null,
                                        "🚨 CẢNH BÁO: Sản phẩm " + ct.getProductId() + " không đủ tồn kho!\n"
                                        + "Khách muốn mua: " + ct.getQuantity() + " | Tồn kho thực tế: " + currentStock,
                                        "Lỗi tồn kho", javax.swing.JOptionPane.ERROR_MESSAGE);
                                con.rollback();
                                return false;
                            }
                        } else {
                            throw new java.sql.SQLException("Không tìm thấy sản phẩm: " + ct.getProductId());
                        }
                    }
                }
            }

            // =========================================================
            // BƯỚC 2: INSERT VÀO BẢNG `ORDERS` (Chuẩn theo Hình 2)
            // =========================================================
            String insertOrderSql = "INSERT INTO ORDERS (order_id, customer_id, employee_id, order_date, total_amount, status, payment_method_id, notes, is_deleted) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";
            try (java.sql.PreparedStatement psOrder = con.prepareStatement(insertOrderSql)) {
                psOrder.setString(1, hoaDon.getOrderId());
                psOrder.setString(2, hoaDon.getCustomerId());
                psOrder.setString(3, hoaDon.getEmployeeId());
                psOrder.setDate(4, hoaDon.getOrderDate());
                psOrder.setDouble(5, hoaDon.getTotalAmount());
                psOrder.setString(6, hoaDon.getStatus());
                psOrder.setString(7, hoaDon.getPaymentMethodId());
                psOrder.setString(8, hoaDon.getNote()); // Nhét vào cột NOTES

                int resOrder = psOrder.executeUpdate();
                if (resOrder <= 0) {
                    throw new java.sql.SQLException("Lỗi lưu hóa đơn chính!");
                }
            }

            // =========================================================
            // BƯỚC 3: INSERT `ORDER_DETAILS` VÀ UPDATE `PRODUCTS` 
            // (Chuẩn theo Hình 1 và Hình 3, không dùng UNIT_ID)
            // =========================================================
            String insertDetailSql = "INSERT INTO ORDER_DETAILS (order_id, product_id, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?)";
            String updateStockSql = "UPDATE PRODUCTS SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

            try (java.sql.PreparedStatement psDetail = con.prepareStatement(insertDetailSql); java.sql.PreparedStatement psUpdateStock = con.prepareStatement(updateStockSql)) {

                for (OrderDetail ct : dsChiTiet) {
                    // 3.1: Lưu Chi Tiết Hóa Đơn (Tự tính LINE_TOTAL = quantity * unit_price)
                    psDetail.setString(1, hoaDon.getOrderId()); // Dùng ID từ hóa đơn cho chắc
                    psDetail.setString(2, ct.getProductId());
                    psDetail.setInt(3, ct.getQuantity());
                    psDetail.setDouble(4, ct.getUnitPrice());
                    psDetail.setDouble(5, ct.getQuantity() * ct.getUnitPrice()); // LINE_TOTAL

                    int resDetail = psDetail.executeUpdate();
                    if (resDetail <= 0) {
                        throw new java.sql.SQLException("Lỗi lưu chi tiết: " + ct.getProductId());
                    }

                    // 3.2: TRỪ TỒN KHO TRỰC TIẾP TRÊN CỘT `STOCK_QUANTITY`
                    psUpdateStock.setInt(1, ct.getQuantity());
                    psUpdateStock.setString(2, ct.getProductId());
                    int resUpdateStock = psUpdateStock.executeUpdate();
                    if (resUpdateStock <= 0) {
                        throw new java.sql.SQLException("Lỗi trừ kho: " + ct.getProductId());
                    }
                }
            }

            // =========================================================
            // BƯỚC 4: HOÀN TẤT VÀ CHỐT ĐƠN (COMMIT)
            // =========================================================
            con.commit();
            System.out.println("✅ Thanh toán hoàn tất! Database đã được cập nhật mượt mà.");
            return true;

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (java.sql.SQLException ex) {
                ex.printStackTrace();
            }
            System.out.println("❌ Thanh toán thất bại: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean cancelOrder(String orderId, String reason) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            Order order = OrdersSql.getInstance().selectById(orderId);
            if (order == null) {
                throw new SQLException("Không tìm thấy đơn hàng!");
            }
            String oldStatus = order.getStatus();

            // Guard: Khong cho phep huy don da CANCELLED de tranh hoan kho nhieu lan
            if ("CANCELLED".equalsIgnoreCase(oldStatus)) {
                throw new SQLException("Don hang " + orderId + " da o trang thai CANCELLED. Khong the huy lai.");
            }

            // 1. Hoàn lại kho
            List<OrderDetail> dsChiTiet = OrderDetailsSql.getInstance().selectByOrderId(orderId);
            for (OrderDetail ct : dsChiTiet) {
                int resAddStock = ProductsSql.getInstance().addStockWithConn(con, ct.getProductId(), ct.getQuantityInBaseUnit());
                if (resAddStock <= 0) {
                    throw new SQLException("Lỗi hoàn kho cho SP: " + ct.getProductId());
                }
            }

            // 2. Cập nhật trạng thái
            int resUpdate = OrdersSql.getInstance().updateStatusWithConn(con, orderId, "CANCELLED");
            if (resUpdate <= 0) {
                throw new SQLException("Lỗi cập nhật trạng thái đơn hàng!");
            }

            con.commit();
            System.out.println("✅ Hủy đơn hàng thành công! Đã hoàn kho.");

            // 3. Ghi log (ngoài transaction chính để tránh ảnh hưởng nếu lỗi log)
            OrderService.logCancelOrder(orderId, oldStatus, "CANCELLED", reason);

            return true;
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.out.println("❌ Hủy đơn hàng thất bại: " + e.getMessage());
            return false;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
