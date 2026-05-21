package business.service;

import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import common.db.DatabaseConnection;
import common.exception.ConcurrentCheckoutException;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import model.order.Order;
import model.order.OrderDetail;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentService {

    public static boolean thanhToan(Order hoaDon, List<OrderDetail> dsChiTiet) {
        Connection con = null;

        try {
            String storeId = requireStoreId(hoaDon);

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            String sqlCheckStock = """
                SELECT i.quantity AS stock_quantity, p.product_name
                FROM INVENTORY i
                JOIN PRODUCTS p ON i.product_id = p.product_id
                WHERE i.product_id = ?
                  AND i.store_id = ?
                  AND NVL(i.is_deleted, 0) = 0
            """;

            for (OrderDetail ct : dsChiTiet) {
                try (PreparedStatement ps = con.prepareStatement(sqlCheckStock)) {
                    ps.setString(1, ct.getProductId());
                    ps.setString(2, storeId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Không tìm thấy sản phẩm tại chi nhánh hiện tại: " + ct.getProductId());
                        }

                        int stock = rs.getInt("stock_quantity");
                        if (stock < ct.getQuantity()) {
                            throw new SQLException(
                                    "Sản phẩm [" + rs.getString("product_name")
                                    + "] không đủ hàng tại chi nhánh hiện tại. Còn: " + stock
                            );
                        }
                    }
                }
            }

            hoaDon.setStoreId(storeId);
            if (hoaDon.getStatus() == null || hoaDon.getStatus().isBlank()) {
                hoaDon.setStatus("Hoàn thành");
            }
            OrdersSql.getInstance().insertWithConn(con, hoaDon);

            String sqlUpdateStock = """
                UPDATE INVENTORY
                SET quantity = quantity - ?, last_updated = SYSDATE
                WHERE product_id = ?
                  AND store_id = ?
                  AND NVL(is_deleted, 0) = 0
                  AND quantity >= ?
            """;

            try (PreparedStatement psStock = con.prepareStatement(sqlUpdateStock)) {
                for (OrderDetail ct : dsChiTiet) {
                    OrderDetailsSql.getInstance().insertWithConn(con, ct);

                    psStock.setInt(1, ct.getQuantity());
                    psStock.setString(2, ct.getProductId());
                    psStock.setString(3, storeId);
                    psStock.setInt(4, ct.getQuantity());

                    int updated = psStock.executeUpdate();
                    if (updated <= 0) {
                        throw new SQLException(
                                "Sản phẩm " + ct.getProductId()
                                + " không đủ tồn kho tại chi nhánh hoặc đã được máy khác bán trước."
                        );
                    }
                }
            }

            if (hoaDon.getCustomerId() != null && !hoaDon.getCustomerId().trim().isEmpty()) {
                CustomersSql.getInstance().recalculateCustomerRank(con, hoaDon.getCustomerId());
            }

            con.commit();
            publishPaymentChanges();
            return true;

        } catch (Exception e) {
            rollbackQuietly(con);
            System.err.println("❌ Lỗi thanh toán: " + e.getMessage());
            e.printStackTrace();
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

            String storeId = requireStoreId(order);

            if ("Đã hủy".equalsIgnoreCase(order.getStatus())
                    || "Đã huỷ".equalsIgnoreCase(order.getStatus())
                    || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
                throw new SQLException("Hóa đơn đã bị hủy trước đó.");
            }

            List<OrderDetail> details = OrderDetailsSql.getInstance().selectByOrderId(orderId);

            String sqlRefill = """
                MERGE INTO INVENTORY i
                USING (
                    SELECT ? AS product_id, ? AS store_id, ? AS quantity FROM dual
                ) src
                ON (i.product_id = src.product_id AND i.store_id = src.store_id)
                WHEN MATCHED THEN
                    UPDATE SET
                        i.quantity = NVL(i.quantity, 0) + src.quantity,
                        i.last_updated = SYSDATE,
                        i.is_deleted = 0
                WHEN NOT MATCHED THEN
                    INSERT (product_id, store_id, quantity, unit, last_updated, is_deleted)
                    VALUES (src.product_id, src.store_id, src.quantity, 'Cái', SYSDATE, 0)
            """;

            try (PreparedStatement ps = con.prepareStatement(sqlRefill)) {
                for (OrderDetail ct : details) {
                    ps.setString(1, ct.getProductId());
                    ps.setString(2, storeId);
                    ps.setInt(3, ct.getQuantity());
                    ps.executeUpdate();
                }
            }

            String oldNote = order.getNote() == null ? "" : order.getNote();
            String newNote = oldNote + " | Lý do hủy: " + reason;

            String sqlUpdateStatus = """
                UPDATE ORDERS
                SET status = N'Đã hủy', note = ?
                WHERE order_id = ?
                  AND store_id = ?
                  AND NVL(is_deleted, 0) = 0
            """;

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateStatus)) {
                ps.setString(1, newNote);
                ps.setString(2, orderId);
                ps.setString(3, storeId);
                int row = ps.executeUpdate();
                if (row <= 0) {
                    throw new SQLException("Không thể cập nhật trạng thái đơn trong chi nhánh hiện tại.");
                }
            }

            logStatusChange(con, orderId, order.getStatus(), "Đã hủy", employeeId, reason);

            if (order.getCustomerId() != null && !order.getCustomerId().trim().isEmpty()) {
                CustomersSql.getInstance().recalculateCustomerRank(con, order.getCustomerId());
            }

            con.commit();
            publishPaymentChanges();
            return true;

        } catch (Exception e) {
            rollbackQuietly(con);
            System.err.println("❌ Lỗi hủy đơn: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConn(con);
        }
    }

    public static boolean processCheckoutSecure(Order order, List<OrderDetail> details) throws ConcurrentCheckoutException {
        String insertDetailSql = """
            INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price, is_deleted)
            VALUES (?, ?, ?, ?, ?, 0)
        """;

        String updateStockSql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?, last_updated = SYSDATE
            WHERE product_id = ?
              AND store_id = ?
              AND quantity >= ?
              AND NVL(is_deleted, 0) = 0
        """;

        String checkStockSql = """
            SELECT quantity
            FROM INVENTORY
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement psDetail = con.prepareStatement(insertDetailSql);
                 PreparedStatement psUpdateStock = con.prepareStatement(updateStockSql);
                 PreparedStatement psCheckStock = con.prepareStatement(checkStockSql)) {

                String storeId = requireStoreId(order);
                order.setStoreId(storeId);
                if (order.getStatus() == null || order.getStatus().isBlank()) {
                    order.setStatus("Hoàn thành");
                }
                OrdersSql.getInstance().insertWithConn(con, order);

                Map<String, Integer> failedItems = new HashMap<>();

                for (OrderDetail d : details) {
                    psUpdateStock.setInt(1, d.getQuantity());
                    psUpdateStock.setString(2, d.getProductId());
                    psUpdateStock.setString(3, storeId);
                    psUpdateStock.setInt(4, d.getQuantity());

                    int updatedRows = psUpdateStock.executeUpdate();

                    if (updatedRows == 0) {
                        psCheckStock.setString(1, d.getProductId());
                        psCheckStock.setString(2, storeId);
                        try (ResultSet rs = psCheckStock.executeQuery()) {
                            failedItems.put(d.getProductId(), rs.next() ? rs.getInt("quantity") : 0);
                        }
                    } else {
                        psDetail.setString(1, "OD" + System.nanoTime());
                        psDetail.setString(2, order.getOrderId());
                        psDetail.setString(3, d.getProductId());
                        psDetail.setInt(4, d.getQuantity());
                        psDetail.setDouble(5, d.getUnitPrice());
                        psDetail.addBatch();
                    }
                }

                if (!failedItems.isEmpty()) {
                    con.rollback();
                    throw new ConcurrentCheckoutException(failedItems);
                }

                psDetail.executeBatch();

                if (order.getCustomerId() != null && !order.getCustomerId().trim().isEmpty()) {
                    CustomersSql.getInstance().updateCustomerAfterPayment(con, order);
                }

                con.commit();
                publishPaymentChanges();
                return true;

            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String requireStoreId(Order order) throws SQLException {
        String storeId = order != null ? order.getStoreId() : null;
        if (storeId == null || storeId.trim().isEmpty()) {
            storeId = SessionManager.getCurrentStoreId();
        }
        if (storeId == null || storeId.trim().isEmpty()) {
            throw new SQLException("Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập bằng tài khoản đã được phân chi nhánh.");
        }
        return storeId.trim();
    }

    private static void publishPaymentChanges() {
        try {
            SyncVersionDao.bumpVersion("CUSTOMERS");
            SyncVersionDao.bumpVersion("ORDERS");
            SyncVersionDao.bumpVersion("INVENTORY");
            SyncVersionDao.bumpVersion("PRODUCTS");

            RealtimeClient.send("CUSTOMERS_CHANGED");
            RealtimeClient.send("ORDERS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");
            RealtimeClient.send("PRODUCTS_CHANGED");

            common.events.EventBus.publish(new common.events.AppDataChangedEvent(common.events.AppEventType.CUSTOMERS, "CUSTOMERS_CHANGED_LOCAL"));
            common.events.EventBus.publish(new common.events.AppDataChangedEvent(common.events.AppEventType.ORDERS, "ORDERS_CHANGED_LOCAL"));
            common.events.EventBus.publish(new common.events.AppDataChangedEvent(common.events.AppEventType.INVENTORY, "INVENTORY_CHANGED_LOCAL"));
            common.events.EventBus.publish(new common.events.AppDataChangedEvent(common.events.AppEventType.PRODUCTS, "PRODUCTS_CHANGED_LOCAL"));
        } catch (Exception ignored) {
        }
    }

    private static void logStatusChange(Connection con, String orderId, String oldStatus, String newStatus, String changedBy, String note) {
        String sql = "INSERT INTO INVOICE_STATUS_LOGS (invoice_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            ps.setString(4, changedBy);
            ps.setString(5, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ Không thể ghi log trạng thái nhưng giao dịch vẫn hoàn tất.");
        }
    }

    private static void rollbackQuietly(Connection con) {
        try {
            if (con != null) {
                con.rollback();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
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
