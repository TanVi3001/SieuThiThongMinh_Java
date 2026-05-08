package business.service;

import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import common.db.DatabaseConnection;
import model.order.Order;
import model.order.OrderDetail;
import java.sql.*;
import java.util.List;
import business.sql.sales_order.CustomersSql;

public class PaymentService {

    public static boolean thanhToan(Order hoaDon, List<OrderDetail> dsChiTiet) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. KIỂM TRA TỒN KHO LẦN CUỐI (TỪ BẢNG INVENTORY)
            for (OrderDetail ct : dsChiTiet) {
                // JOIN bảng INVENTORY và PRODUCTS để lấy tên SP và số lượng tồn kho
                String sql = "SELECT i.quantity AS stock_quantity, p.product_name "
                        + "FROM INVENTORY i "
                        + "JOIN PRODUCTS p ON i.product_id = p.product_id "
                        + "WHERE i.product_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ct.getProductId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int stock = rs.getInt("stock_quantity");
                            if (stock < ct.getQuantity()) {
                                throw new SQLException("Sản phẩm [" + rs.getString("product_name") + "] không đủ hàng (Còn: " + stock + ")");
                            }
                        } else {
                            throw new SQLException("Không tìm thấy thông tin tồn kho cho SP: " + ct.getProductId());
                        }
                    }
                }
            }

            // 2. LƯU HÓA ĐƠN CHÍNH
            String sqlOrder = "INSERT INTO ORDERS (order_id, customer_id, employee_id, order_date, total_amount, status, payment_method_id, note, is_deleted) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";
            try (PreparedStatement ps = con.prepareStatement(sqlOrder)) {
                ps.setString(1, hoaDon.getOrderId());
                ps.setString(2, hoaDon.getCustomerId());
                ps.setString(3, hoaDon.getEmployeeId());
                ps.setDate(4, hoaDon.getOrderDate());
                ps.setDouble(5, hoaDon.getTotalAmount());
                ps.setString(6, hoaDon.getStatus()); // Đã lưu thẳng Tiếng Việt
                ps.setString(7, hoaDon.getPaymentMethodId());
                ps.setString(8, hoaDon.getNote());
                if (ps.executeUpdate() <= 0) {
                    throw new SQLException("Không thể tạo hóa đơn.");
                }
            }

            // 3. LƯU CHI TIẾT & TRỪ KHO (CẬP NHẬT BẢNG INVENTORY)
            String sqlUpdateStock = "UPDATE INVENTORY SET quantity = quantity - ? WHERE product_id = ?";

            try (PreparedStatement psSt = con.prepareStatement(sqlUpdateStock)) {
                for (OrderDetail ct : dsChiTiet) {
                    // FIX LỖI ORA-00904: Dùng hàm của DAO để tận dụng cơ chế bắt lỗi thông minh
                    OrderDetailsSql.getInstance().insertWithConn(con, ct);

                    // Trừ kho
                    psSt.setInt(1, ct.getQuantity());
                    psSt.setString(2, ct.getProductId());
                    psSt.executeUpdate();
                }
            }
            // 4. UPDATE KHÁCH HÀNG THÀNH VIÊN
            CustomersSql.getInstance().updateCustomerAfterPayment(con, hoaDon);

            con.commit();
            return true;
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Lỗi thanh toán: " + e.getMessage());
            return false;
        } finally {
            closeConn(con);
        }
    }

    public static boolean cancelOrder(String orderId, String employeeId, String reason) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            Order order = OrdersSql.getInstance().selectById(orderId);
            if (order == null) {
                throw new SQLException("Không tìm thấy hóa đơn.");
            }
            if ("Đã hủy".equalsIgnoreCase(order.getStatus()) || "Đã huỷ".equalsIgnoreCase(order.getStatus())) {
                throw new SQLException("Hóa đơn đã hủy rồi.");
            }

            // Hoàn tồn kho (BẢNG INVENTORY)
            List<OrderDetail> details = OrderDetailsSql.getInstance().selectByOrderId(orderId);
            String sqlRefill = "UPDATE INVENTORY SET quantity = quantity + ? WHERE product_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlRefill)) {
                for (OrderDetail ct : details) {
                    ps.setInt(1, ct.getQuantity());
                    ps.setString(2, ct.getProductId());
                    ps.executeUpdate();
                }
            }

            // Cập nhật trạng thái Tiếng Việt
            String sqlUpdateStatus
                    = "UPDATE ORDERS "
                    + "SET status = 'Đã hủy', "
                    + "note = NVL(note, '') || ?, "
                    + "updated_at = CURRENT_TIMESTAMP "
                    + "WHERE order_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateStatus)) {

                ps.setString(1, " | Lý do hủy: " + reason);
                ps.setString(2, orderId);

                ps.executeUpdate();
            }
            logStatusChange(con, orderId, order.getStatus(), "Đã hủy", employeeId, reason);
            CustomersSql.getInstance().recalculateCustomerRank(con, order.getCustomerId());
            con.commit();
            return true;
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Lỗi hủy đơn: " + e.getMessage());
            return false;
        } finally {
            closeConn(con);
        }
    }

    private static void logStatusChange(Connection con, String orderId, String oldSt, String newSt, String user, String note) {
        String sql = "INSERT INTO INVOICE_STATUS_LOGS (invoice_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, oldSt);
            ps.setString(3, newSt);
            ps.setString(4, user);
            ps.setString(5, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ Không thể ghi log nhưng đơn vẫn được xử lý.");
        }
    }

    private static void closeConn(Connection con) {
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
