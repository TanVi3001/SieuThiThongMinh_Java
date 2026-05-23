package business.service.invoice;

import business.sql.sales_order.CustomersSql;
import common.realtime.RealtimeNotifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceUpdateService {

    private final Connection conn;

    public InvoiceUpdateService(Connection conn) {
        this.conn = conn;
    }

    public static class EditedOrderDetail {

        private final String productId;
        private final int quantity;
        private final double unitPrice;

        public EditedOrderDetail(String productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }
    }

    public void saveInvoiceChanges(String orderId, List<EditedOrderDetail> editedDetails) throws Exception {
        if (conn == null) {
            throw new IllegalStateException("Connection null.");
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        }

        if (editedDetails == null || editedDetails.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất một sản phẩm.");
        }

        boolean oldAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            OrderHeaderInfo orderInfo = lockOrder(orderId);
            Map<String, Integer> oldQtyMap = loadOldQuantities(orderId);

            for (EditedOrderDetail detail : editedDetails) {
                validateDetail(detail);

                int oldQty = oldQtyMap.getOrDefault(detail.getProductId(), 0);
                int newQty = detail.getQuantity();
                int diff = newQty - oldQty;

                upsertOrderDetail(orderId, detail);

                if (diff != 0) {
                    updateInventoryByDiff(orderInfo.storeId, detail.getProductId(), diff);
                }

                oldQtyMap.remove(detail.getProductId());
            }

            // Những sản phẩm cũ không còn trong bảng UI thì xóa mềm và hoàn kho.
            for (Map.Entry<String, Integer> removed : oldQtyMap.entrySet()) {
                String removedProductId = removed.getKey();
                int oldQty = removed.getValue();

                softDeleteOrderDetail(orderId, removedProductId);
                restoreInventory(orderInfo.storeId, removedProductId, oldQty);
            }

            double newTotal = recalculateOrderTotal(orderId);
            updateOrderTotal(orderId, newTotal);

            if (orderInfo.customerId != null && !orderInfo.customerId.trim().isEmpty()) {
                CustomersSql.getInstance().recalculateCustomerRank(conn, orderInfo.customerId);
            }

            conn.commit();

            RealtimeNotifier.ordersChanged("ORDER_DETAIL_UPDATED:" + orderId);
            RealtimeNotifier.inventoryChanged("STOCK_UPDATED_BY_ORDER_EDIT:" + orderId);
            RealtimeNotifier.customersChanged("CUSTOMER_SPENDING_RECALCULATED_BY_ORDER_EDIT:" + orderId);

        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private void validateDetail(EditedOrderDetail detail) {
        if (detail == null) {
            throw new IllegalArgumentException("Chi tiết hóa đơn không hợp lệ.");
        }

        if (detail.getProductId() == null || detail.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ.");
        }

        if (detail.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0: " + detail.getProductId());
        }

        if (detail.getUnitPrice() < 0) {
            throw new IllegalArgumentException("Đơn giá không hợp lệ: " + detail.getProductId());
        }
    }

    private OrderHeaderInfo lockOrder(String orderId) throws SQLException {
        String sql = """
            SELECT order_id,
                   customer_id,
                   store_id,
                   status
            FROM ORDERS
            WHERE order_id = ?
              AND NVL(is_deleted, 0) = 0
            FOR UPDATE
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy hóa đơn: " + orderId);
                }

                String status = rs.getString("status");

                if (isCancelled(status)) {
                    throw new SQLException("Không thể sửa hóa đơn đã hủy.");
                }

                OrderHeaderInfo info = new OrderHeaderInfo();
                info.orderId = rs.getString("order_id");
                info.customerId = rs.getString("customer_id");
                info.storeId = rs.getString("store_id");

                if (info.storeId == null || info.storeId.trim().isEmpty()) {
                    throw new SQLException("Hóa đơn chưa có store_id nên không thể cập nhật tồn kho.");
                }

                return info;
            }
        }
    }

    private Map<String, Integer> loadOldQuantities(String orderId) throws SQLException {
        Map<String, Integer> map = new HashMap<>();

        String sql = """
            SELECT product_id,
                   NVL(quantity, 0) AS quantity
            FROM ORDER_DETAILS
            WHERE order_id = ?
              AND NVL(is_deleted, 0) = 0
            FOR UPDATE
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("product_id"), rs.getInt("quantity"));
                }
            }
        }

        return map;
    }

    private void upsertOrderDetail(String orderId, EditedOrderDetail detail) throws SQLException {
        String updateSql = """
            UPDATE ORDER_DETAILS
            SET quantity = ?,
                unit_price = ?,
                is_deleted = 0
            WHERE order_id = ?
              AND product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, detail.getQuantity());
            ps.setDouble(2, detail.getUnitPrice());
            ps.setString(3, orderId);
            ps.setString(4, detail.getProductId());

            int updated = ps.executeUpdate();

            if (updated > 0) {
                return;
            }
        }

        String insertSql = """
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

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, "OD" + System.nanoTime());
            ps.setString(2, orderId);
            ps.setString(3, detail.getProductId());
            ps.setInt(4, detail.getQuantity());
            ps.setDouble(5, detail.getUnitPrice());
            ps.executeUpdate();
        }
    }

    private void softDeleteOrderDetail(String orderId, String productId) throws SQLException {
        String sql = """
            UPDATE ORDER_DETAILS
            SET is_deleted = 1
            WHERE order_id = ?
              AND product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    private void updateInventoryByDiff(String storeId, String productId, int diff) throws SQLException {
        // diff > 0 nghĩa là tăng số lượng trong hóa đơn, phải trừ thêm kho.
        // diff < 0 nghĩa là giảm số lượng trong hóa đơn, phải hoàn lại kho.
        if (diff > 0) {
            decreaseInventory(storeId, productId, diff);
        } else {
            restoreInventory(storeId, productId, Math.abs(diff));
        }
    }

    private void decreaseInventory(String storeId, String productId, int qty) throws SQLException {
        String sql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?,
                last_updated = SYSDATE
            WHERE store_id = ?
              AND product_id = ?
              AND NVL(is_deleted, 0) = 0
              AND quantity >= ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setString(2, storeId);
            ps.setString(3, productId);
            ps.setInt(4, qty);

            int updated = ps.executeUpdate();

            if (updated <= 0) {
                throw new SQLException("Không đủ tồn kho để tăng số lượng sản phẩm: " + productId);
            }
        }
    }

    private void restoreInventory(String storeId, String productId, int qty) throws SQLException {
        String sql = """
            MERGE INTO INVENTORY i
            USING (
                SELECT ? AS store_id,
                       ? AS product_id,
                       ? AS qty
                FROM dual
            ) src
            ON (
                i.store_id = src.store_id
                AND i.product_id = src.product_id
            )
            WHEN MATCHED THEN
                UPDATE SET
                    i.quantity = NVL(i.quantity, 0) + src.qty,
                    i.last_updated = SYSDATE,
                    i.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (
                    store_id,
                    product_id,
                    quantity,
                    unit,
                    last_updated,
                    is_deleted
                )
                VALUES (
                    src.store_id,
                    src.product_id,
                    src.qty,
                    'Cái',
                    SYSDATE,
                    0
                )
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, storeId);
            ps.setString(2, productId);
            ps.setInt(3, qty);
            ps.executeUpdate();
        }
    }

    private double recalculateOrderTotal(String orderId) throws SQLException {
        String sql = """
            SELECT NVL(SUM(NVL(quantity, 0) * NVL(unit_price, 0)), 0) AS total_amount
            FROM ORDER_DETAILS
            WHERE order_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_amount");
                }
            }
        }

        return 0;
    }

    private void updateOrderTotal(String orderId, double totalAmount) throws SQLException {
        String sql = """
            UPDATE ORDERS
            SET total_amount = ?
            WHERE order_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, totalAmount);
            ps.setString(2, orderId);

            int updated = ps.executeUpdate();

            if (updated <= 0) {
                throw new SQLException("Không thể cập nhật tổng tiền hóa đơn: " + orderId);
            }
        }
    }

    private boolean isCancelled(String status) {
        if (status == null) {
            return false;
        }

        String s = status.trim().toLowerCase();

        return s.contains("hủy")
                || s.contains("huỷ")
                || s.contains("huy")
                || s.equals("cancelled");
    }

    private static class OrderHeaderInfo {

        String orderId;
        String customerId;
        String storeId;
    }
}
