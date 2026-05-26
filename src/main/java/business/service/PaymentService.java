package business.service;

import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import common.db.DatabaseConnection;
import common.exception.ConcurrentCheckoutException;
import common.realtime.RealtimeNotifier;
import model.order.Order;
import model.order.OrderDetail;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentService {

    public static boolean thanhToan(Order hoaDon, List<OrderDetail> dsChiTiet) {
        Connection con = null;

        try {
            validateCheckoutInput(hoaDon, dsChiTiet);

            String storeId = requireStoreId(hoaDon);
            assertWritableStore(storeId);

            normalizeOrderBeforeCheckout(hoaDon, storeId);

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            checkStockBeforeCheckout(con, storeId, dsChiTiet);

            OrdersSql.getInstance().insertWithConn(con, hoaDon);

            insertOrderDetailsAndSubtractStock(con, hoaDon, dsChiTiet, storeId);

            if (hasText(hoaDon.getCustomerId())) {
                CustomersSql.getInstance().recalculateCustomerRank(con, hoaDon.getCustomerId());
            }

            con.commit();

            publishPaymentChanges(
                    "PAYMENT_SUCCESS:" + hoaDon.getOrderId() + ":STORE:" + storeId,
                    hoaDon.getOrderId(),
                    storeId,
                    hoaDon.getCustomerId()
            );

            return true;

        } catch (Exception e) {
            rollbackQuietly(con);
            System.err.println("Lỗi thanh toán: " + e.getMessage());
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

            Order order;

            if (SessionManager.isAdmin()) {
                order = OrdersSql.getInstance().selectById(orderId);
            } else {
                String currentStoreId = SessionManager.getCurrentStoreId();

                if (!hasText(currentStoreId)) {
                    throw new SQLException("Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.");
                }

                order = OrdersSql.getInstance().selectByIdInStore(orderId, currentStoreId.trim());
            }

            if (order == null) {
                throw new SQLException("Không tìm thấy hóa đơn hoặc bạn không có quyền thao tác hóa đơn này.");
            }

            String storeId = requireStoreId(order);
            assertWritableStore(storeId);

            if (isCancelledStatus(order.getStatus())) {
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
            String cancelReason = !hasText(reason) ? "Không có lý do" : reason.trim();
            String newNote = oldNote + " | Lý do hủy: " + cancelReason;

            String sqlUpdateStatus = """
                UPDATE ORDERS
                SET status = N'Đã hủy',
                    note = ?
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

            logStatusChange(con, orderId, order.getStatus(), "Đã hủy", employeeId, cancelReason);

            if (hasText(order.getCustomerId())) {
                CustomersSql.getInstance().recalculateCustomerRank(con, order.getCustomerId());
            }

            con.commit();

            publishPaymentChanges(
                    "ORDER_CANCELLED:" + orderId + ":STORE:" + storeId,
                    orderId,
                    storeId,
                    order.getCustomerId()
            );

            return true;

        } catch (Exception e) {
            rollbackQuietly(con);
            System.err.println("Lỗi hủy đơn: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConn(con);
        }
    }

    public static boolean processCheckoutSecure(Order order, List<OrderDetail> details) throws ConcurrentCheckoutException {
        Connection con = null;

        try {
            validateCheckoutInput(order, details);

            String storeId = requireStoreId(order);
            assertWritableStore(storeId);
            normalizeOrderBeforeCheckout(order, storeId);

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            OrdersSql.getInstance().insertWithConn(con, order);

            Map<String, Integer> failedItems = new HashMap<>();

            String checkStockSql = """
                SELECT quantity
                FROM INVENTORY
                WHERE product_id = ?
                  AND store_id = ?
                  AND NVL(is_deleted, 0) = 0
            """;

            String updateStockSql = """
                UPDATE INVENTORY
                SET quantity = quantity - ?,
                    last_updated = SYSDATE
                WHERE product_id = ?
                  AND store_id = ?
                  AND quantity >= ?
                  AND NVL(is_deleted, 0) = 0
            """;

            String insertDetailSql = """
                INSERT INTO ORDER_DETAILS (
                    order_detail_id,
                    order_id,
                    product_id,
                    quantity,
                    unit_price,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, 0)
            """;

            try (
                    PreparedStatement psCheckStock = con.prepareStatement(checkStockSql); PreparedStatement psUpdateStock = con.prepareStatement(updateStockSql); PreparedStatement psDetail = con.prepareStatement(insertDetailSql)) {
                for (OrderDetail d : details) {
                    normalizeOrderDetailBeforeInsert(d, order.getOrderId());

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
                        psDetail.setString(1, buildOrderDetailId());
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
            }

            if (hasText(order.getCustomerId())) {
                CustomersSql.getInstance().updateCustomerAfterPayment(con, order);
            }

            con.commit();

            publishPaymentChanges(
                    "PAYMENT_SUCCESS:" + order.getOrderId() + ":STORE:" + storeId,
                    order.getOrderId(),
                    storeId,
                    order.getCustomerId()
            );

            return true;

        } catch (ConcurrentCheckoutException e) {
            rollbackQuietly(con);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(con);
            System.err.println("Lỗi processCheckoutSecure: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConn(con);
        }
    }

    private static final ExecutorService REALTIME_NOTIFY_EXECUTOR
            = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "payment-realtime-notifier");
                t.setDaemon(true);
                return t;
            });

    private static void validateCheckoutInput(Order order, List<OrderDetail> details) throws SQLException {
        if (order == null) {
            throw new SQLException("Hóa đơn không hợp lệ.");
        }

        if (!hasText(order.getOrderId())) {
            throw new SQLException("Hóa đơn chưa có mã đơn.");
        }

        if (details == null || details.isEmpty()) {
            throw new SQLException("Giỏ hàng đang rỗng, không thể tạo hóa đơn.");
        }

        for (OrderDetail d : details) {
            if (d == null) {
                throw new SQLException("Chi tiết hóa đơn không hợp lệ.");
            }

            if (!hasText(d.getProductId())) {
                throw new SQLException("Có sản phẩm trong giỏ chưa có mã sản phẩm.");
            }

            if (d.getQuantity() <= 0) {
                throw new SQLException("Số lượng sản phẩm phải lớn hơn 0: " + d.getProductId());
            }

            if (d.getUnitPrice() < 0) {
                throw new SQLException("Đơn giá sản phẩm không hợp lệ: " + d.getProductId());
            }
        }
    }

    private static void normalizeOrderBeforeCheckout(Order order, String storeId) throws SQLException {
        order.setStoreId(storeId);

        String currentEmployeeId = getCurrentEmployeeIdOrNull();

        if (!hasText(order.getEmployeeId()) && currentEmployeeId != null) {
            order.setEmployeeId(currentEmployeeId);
        }

        if (!hasText(order.getEmployeeId())) {
            throw new SQLException("Không xác định được nhân viên bán hàng hiện tại.");
        }

        if (!hasText(order.getStatus())) {
            order.setStatus("Hoàn thành");
        }
    }

    private static void normalizeOrderDetailBeforeInsert(OrderDetail detail, String orderId) {
        try {
            detail.setOrderId(orderId);
        } catch (Exception ignored) {
        }

        try {
            if (!hasText(detail.getOrderDetailId())) {
                detail.setOrderDetailId(buildOrderDetailId());
            }
        } catch (Exception ignored) {
        }
    }

    private static void checkStockBeforeCheckout(
            Connection con,
            String storeId,
            List<OrderDetail> details
    ) throws SQLException {
        String sqlCheckStock = """
            SELECT i.quantity AS stock_quantity,
                   p.product_name
            FROM INVENTORY i
            JOIN PRODUCTS p
                ON i.product_id = p.product_id
            WHERE i.product_id = ?
              AND i.store_id = ?
              AND NVL(i.is_deleted, 0) = 0
        """;

        try (PreparedStatement ps = con.prepareStatement(sqlCheckStock)) {
            for (OrderDetail ct : details) {
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
    }

    private static void insertOrderDetailsAndSubtractStock(
            Connection con,
            Order order,
            List<OrderDetail> details,
            String storeId
    ) throws SQLException {
        String insertDetailSql = """
            INSERT INTO ORDER_DETAILS (
                order_detail_id,
                order_id,
                product_id,
                quantity,
                unit_price,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, 0)
        """;

        String updateStockSql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?,
                last_updated = SYSDATE
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
              AND quantity >= ?
        """;

        try (
                PreparedStatement psDetail = con.prepareStatement(insertDetailSql); PreparedStatement psStock = con.prepareStatement(updateStockSql)) {
            for (OrderDetail ct : details) {
                normalizeOrderDetailBeforeInsert(ct, order.getOrderId());

                psDetail.setString(1, ct.getOrderDetailId());
                psDetail.setString(2, order.getOrderId());
                psDetail.setString(3, ct.getProductId());
                psDetail.setInt(4, ct.getQuantity());
                psDetail.setDouble(5, ct.getUnitPrice());
                psDetail.addBatch();

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

            psDetail.executeBatch();
        }
    }

    private static String requireStoreId(Order order) throws SQLException {
        String storeId = order != null ? order.getStoreId() : null;

        if (!hasText(storeId)) {
            storeId = SessionManager.getCurrentStoreId();
        }

        if (!hasText(storeId)) {
            throw new SQLException("Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập bằng tài khoản đã được phân chi nhánh.");
        }

        return storeId.trim();
    }

    private static void assertWritableStore(String storeId) throws SQLException {
        if (SessionManager.isAdmin()) {
            return;
        }

        String currentStoreId = SessionManager.getCurrentStoreId();

        if (!hasText(currentStoreId)) {
            throw new SQLException("Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.");
        }

        if (storeId == null || !currentStoreId.trim().equalsIgnoreCase(storeId.trim())) {
            throw new SQLException("Bạn không có quyền thao tác hóa đơn/tồn kho của chi nhánh khác.");
        }
    }

    private static String getCurrentEmployeeIdOrNull() {
        try {
            String employeeId = SessionManager.getCurrentEmployeeId();

            if (hasText(employeeId)) {
                return employeeId.trim();
            }
        } catch (Exception ignored) {
        }

        try {
            if (SessionManager.getCurrentUser() != null
                    && hasText(SessionManager.getCurrentUser().getUserId())) {
                return SessionManager.getCurrentUser().getUserId().trim();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String buildOrderDetailId() {
        return "OD" + System.nanoTime();
    }

    private static boolean isCancelledStatus(String status) {
        if (status == null) {
            return false;
        }

        String s = status.trim().toLowerCase();

        return s.contains("hủy")
                || s.contains("huỷ")
                || s.contains("huy")
                || s.equals("cancelled");
    }

    /**
     * Realtime sau thanh toán/hủy đơn: - ORDERS: luôn reload vì hóa đơn thay
     * đổi. - INVENTORY: luôn reload vì tồn kho bị trừ/hoàn. - CUSTOMERS: chỉ
     * reload nếu có customerId thật, tránh guest order làm lag CustomerView.
     *
     * Dashboard/Report không gọi trực tiếp ở đây; RealtimeNotifier tự debounce
     * theo ordersChanged().
     */
    private static void publishPaymentChanges(
            String message,
            String orderId,
            String storeId,
            String customerId
    ) {
        REALTIME_NOTIFY_EXECUTOR.submit(() -> {
            try {
                String baseMessage = !hasText(message) ? "PAYMENT_CHANGED" : message.trim();
                String orderToken = hasText(orderId) ? ":ORDER:" + orderId.trim() : "";
                String storeToken = hasText(storeId) ? ":STORE:" + storeId.trim() : "";

                // Chỉ bắn sự kiện cần thiết, chạy nền nên không chặn thanh toán/xuất hóa đơn
                RealtimeNotifier.ordersChanged(
                        "ORDER_PAYMENT_CHANGED:" + baseMessage + orderToken + storeToken
                );

                RealtimeNotifier.inventoryChanged(
                        "INVENTORY_BY_ORDER:" + baseMessage + orderToken + storeToken
                );

                if (hasText(customerId)) {
                    RealtimeNotifier.customersChanged(
                            "CUSTOMER_BY_ORDER:" + baseMessage
                            + ":CUSTOMER:" + customerId.trim()
                            + orderToken
                            + storeToken
                    );
                }

            } catch (Exception ex) {
                System.err.println("[PaymentService] realtime notify async error: " + ex.getMessage());
            }
        });
    }

    private static void logStatusChange(
            Connection con,
            String orderId,
            String oldStatus,
            String newStatus,
            String changedBy,
            String note
    ) {
        String sql = """
            INSERT INTO INVOICE_STATUS_LOGS (
                invoice_id,
                old_status,
                new_status,
                changed_by,
                note
            )
            VALUES (?, ?, ?, ?, ?)
        """;

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

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
