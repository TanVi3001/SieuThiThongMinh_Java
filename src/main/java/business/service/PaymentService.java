package business.service;

import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import common.db.DatabaseConnection;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import model.order.Order;
import model.order.OrderDetail;

import java.sql.*;
import java.util.List;

public class PaymentService {

    public static boolean thanhToan(Order hoaDon, List<OrderDetail> dsChiTiet) {

        Connection con = null;

        try {

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // =========================================================
            // 1. CHECK TỒN KHO
            // =========================================================
            String sqlCheckStock
                    = "SELECT i.quantity AS stock_quantity, p.product_name "
                    + "FROM INVENTORY i "
                    + "JOIN PRODUCTS p ON i.product_id = p.product_id "
                    + "WHERE i.product_id = ?";

            for (OrderDetail ct : dsChiTiet) {

                try (PreparedStatement ps = con.prepareStatement(sqlCheckStock)) {

                    ps.setString(1, ct.getProductId());

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Không tìm thấy sản phẩm: " + ct.getProductId());
                        }

                        int stock = rs.getInt("stock_quantity");

                        if (stock < ct.getQuantity()) {
                            throw new SQLException(
                                    "Sản phẩm [" + rs.getString("product_name")
                                    + "] không đủ hàng. Còn: " + stock
                            );
                        }
                    }
                }
            }

            // =========================================================
            // 2. INSERT HÓA ĐƠN
            // =========================================================
            String sqlOrder
                    = "INSERT INTO ORDERS ("
                    + "order_id, customer_id, employee_id, "
                    + "order_date, total_amount, status, "
                    + "payment_method_id, note, is_deleted"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";

            try (PreparedStatement ps = con.prepareStatement(sqlOrder)) {

                ps.setString(1, hoaDon.getOrderId());
                ps.setString(2, hoaDon.getCustomerId());
                ps.setString(3, hoaDon.getEmployeeId());
                ps.setDate(4, hoaDon.getOrderDate());
                ps.setDouble(5, hoaDon.getTotalAmount());

                // QUAN TRỌNG
                ps.setString(6, "Hoàn thành");

                ps.setString(7, hoaDon.getPaymentMethodId());
                ps.setString(8, hoaDon.getNote());

                int row = ps.executeUpdate();

                if (row <= 0) {
                    throw new SQLException("Không thể tạo hóa đơn.");
                }
            }

            // =========================================================
            // 3. INSERT CHI TIẾT + TRỪ KHO
            // =========================================================
            String sqlUpdateStock
                    = "UPDATE INVENTORY "
                    + "SET quantity = quantity - ? "
                    + "WHERE product_id = ?";

            try (PreparedStatement psStock = con.prepareStatement(sqlUpdateStock)) {

                for (OrderDetail ct : dsChiTiet) {

                    // INSERT ORDER DETAIL
                    OrderDetailsSql.getInstance().insertWithConn(con, ct);

                    // UPDATE STOCK
                    psStock.setInt(1, ct.getQuantity());
                    psStock.setString(2, ct.getProductId());

                    int updated = psStock.executeUpdate();

                    if (updated <= 0) {
                        throw new SQLException("Không thể cập nhật tồn kho.");
                    }
                }
            }

            // =========================================================
            // 4. RECALCULATE CUSTOMER
            // =========================================================
            if (hoaDon.getCustomerId() != null
                    && !hoaDon.getCustomerId().trim().isEmpty()) {

                CustomersSql.getInstance()
                        .recalculateCustomerRank(con, hoaDon.getCustomerId());
            }

            // =========================================================
            // 5. COMMIT
            // =========================================================
            con.commit();

            // =========================================================
            // 6. REALTIME + VERSION
            // =========================================================
            SyncVersionDao.bumpVersion("CUSTOMERS");
            SyncVersionDao.bumpVersion("ORDERS");
            SyncVersionDao.bumpVersion("INVENTORY");

            RealtimeClient.send("CUSTOMERS_CHANGED");
            RealtimeClient.send("ORDERS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");

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
            e.printStackTrace();

            return false;

        } finally {
            closeConn(con);
        }
    }

    public static boolean cancelOrder(
            String orderId,
            String employeeId,
            String reason
    ) {

        Connection con = null;

        try {

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // =========================================================
            // 1. LẤY HÓA ĐƠN
            // =========================================================
            Order order = OrdersSql.getInstance().selectById(orderId);

            if (order == null) {
                throw new SQLException("Không tìm thấy hóa đơn.");
            }

            if ("Đã hủy".equalsIgnoreCase(order.getStatus())
                    || "Đã huỷ".equalsIgnoreCase(order.getStatus())) {

                throw new SQLException("Hóa đơn đã bị hủy trước đó.");
            }

            // =========================================================
            // 2. HOÀN KHO
            // =========================================================
            List<OrderDetail> details
                    = OrderDetailsSql.getInstance().selectByOrderId(orderId);

            String sqlRefill
                    = "UPDATE INVENTORY "
                    + "SET quantity = quantity + ? "
                    + "WHERE product_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlRefill)) {

                for (OrderDetail ct : details) {

                    ps.setInt(1, ct.getQuantity());
                    ps.setString(2, ct.getProductId());

                    ps.executeUpdate();
                }
            }

            // =========================================================
            // 3. UPDATE STATUS (Sửa lại an toàn tuyệt đối cho Oracle)
            // =========================================================
            String oldNote = order.getNote() == null ? "" : order.getNote();
            String newNote = oldNote + " | Lý do hủy: " + reason;

            String sqlUpdateStatus
                    = "UPDATE ORDERS "
                    + "SET status = N'Đã hủy', "
                    + "note = ? "
                    + "WHERE order_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateStatus)) {

                ps.setString(1, newNote);
                ps.setString(2, orderId);

                int row = ps.executeUpdate();

                if (row <= 0) {
                    throw new SQLException("Không thể cập nhật trạng thái đơn.");
                }
            }

            // =========================================================
            // 4. LOG
            // =========================================================
            logStatusChange(
                    con,
                    orderId,
                    order.getStatus(),
                    "Đã hủy",
                    employeeId,
                    reason
            );

            // =========================================================
            // 5. RECALCULATE CUSTOMER
            // =========================================================
            if (order.getCustomerId() != null
                    && !order.getCustomerId().trim().isEmpty()) {

                CustomersSql.getInstance()
                        .recalculateCustomerRank(con, order.getCustomerId());
            }

            // =========================================================
            // 6. COMMIT
            // =========================================================
            con.commit();

            // =========================================================
            // 7. REALTIME
            // =========================================================
            SyncVersionDao.bumpVersion("CUSTOMERS");
            SyncVersionDao.bumpVersion("ORDERS");
            SyncVersionDao.bumpVersion("INVENTORY");

            RealtimeClient.send("CUSTOMERS_CHANGED");
            RealtimeClient.send("ORDERS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");

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
            e.printStackTrace();

            return false;

        } finally {
            closeConn(con);
        }
    }

    private static void logStatusChange(
            Connection con,
            String orderId,
            String oldStatus,
            String newStatus,
            String changedBy,
            String note
    ) {

        String sql
                = "INSERT INTO INVOICE_STATUS_LOGS ("
                + "invoice_id, old_status, new_status, changed_by, note"
                + ") VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, orderId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            ps.setString(4, changedBy);
            ps.setString(5, note);

            ps.executeUpdate();

        } catch (SQLException e) {

            System.err.println(
                    "⚠️ Không thể ghi log trạng thái nhưng giao dịch vẫn hoàn tất."
            );
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
