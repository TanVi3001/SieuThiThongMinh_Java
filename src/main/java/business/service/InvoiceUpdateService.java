package business.service;

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

        private final String orderDetailId;
        private final String productId;
        private final String unitId;
        private final int quantity;
        private final int quantityBase;
        private final double unitPrice;

        public EditedOrderDetail(String productId, int quantity, double unitPrice) {
            this(null, productId, null, quantity, quantity, unitPrice);
        }

        public EditedOrderDetail(String orderDetailId, String productId, String unitId,
                int quantity, int quantityBase, double unitPrice) {
            this.orderDetailId = orderDetailId;
            this.productId = productId;
            this.unitId = unitId;
            this.quantity = quantity;
            this.quantityBase = quantityBase;
            this.unitPrice = unitPrice;
        }

        public String getOrderDetailId() {
            return orderDetailId;
        }

        public String getProductId() {
            return productId;
        }

        public String getUnitId() {
            return unitId;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getQuantityBase() {
            return quantityBase > 0 ? quantityBase : quantity;
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
            Map<String, OldDetailInfo> oldDetailMap = loadOldDetails(orderId);

            for (EditedOrderDetail detail : editedDetails) {
                validateDetail(detail);

                String detailKey = normalizeDetailKey(detail);
                OldDetailInfo old = oldDetailMap.get(detailKey);

                int oldBaseQty = old != null ? old.quantityBase : 0;
                int newBaseQty = detail.getQuantityBase();
                int diffBaseQty = newBaseQty - oldBaseQty;

                upsertOrderDetail(orderId, detail);

                if (diffBaseQty != 0) {
                    updateInventoryByDiff(orderInfo.storeId, detail.getProductId(), diffBaseQty);
                }

                oldDetailMap.remove(detailKey);
            }

            for (OldDetailInfo old : oldDetailMap.values()) {
                softDeleteOrderDetail(old.orderDetailId);
                restoreInventory(orderInfo.storeId, old.productId, old.quantityBase);
            }

            double newTotal = recalculateOrderTotal(orderId);
            updateOrderTotal(orderId, newTotal);

            if (orderInfo.customerId != null && !orderInfo.customerId.trim().isEmpty()) {
                CustomersSql.getInstance().recalculateCustomerRank(conn, orderInfo.customerId);
            }

            conn.commit();

            notifyChangesAsync(orderId);

        } catch (Exception ex) {
            conn.rollback();
            throw ex;

        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private void notifyChangesAsync(String orderId) {
        Thread notifier = new Thread(() -> {
            try {
                RealtimeNotifier.ordersChanged("ORDER_DETAIL_UPDATED:" + orderId);
                RealtimeNotifier.inventoryChanged("STOCK_UPDATED_BY_ORDER_EDIT:" + orderId);
                RealtimeNotifier.customersChanged("CUSTOMER_SPENDING_RECALCULATED_BY_ORDER_EDIT:" + orderId);
            } catch (Exception ex) {
                System.err.println("Realtime notify failed after invoice update: " + ex.getMessage());
            }
        }, "invoice-update-notifier");

        notifier.setDaemon(true);
        notifier.start();
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

        if (detail.getQuantityBase() <= 0) {
            throw new IllegalArgumentException("Số lượng quy đổi phải lớn hơn 0: " + detail.getProductId());
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

    private Map<String, OldDetailInfo> loadOldDetails(String orderId) throws SQLException {
        Map<String, OldDetailInfo> map = new HashMap<>();

        String sql = """
            SELECT order_detail_id,
                   product_id,
                   unit_id,
                   NVL(quantity, 0) AS quantity,
                   NVL(quantity_base, NVL(quantity, 0)) AS quantity_base
            FROM ORDER_DETAILS
            WHERE order_id = ?
              AND NVL(is_deleted, 0) = 0
            FOR UPDATE
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OldDetailInfo info = new OldDetailInfo();
                    info.orderDetailId = rs.getString("order_detail_id");
                    info.productId = rs.getString("product_id");
                    info.unitId = rs.getString("unit_id");
                    info.quantity = rs.getInt("quantity");
                    info.quantityBase = rs.getInt("quantity_base");

                    map.put(normalizeDetailKey(info.orderDetailId, info.productId, info.unitId), info);
                }
            }
        }

        return map;
    }

    private String normalizeDetailKey(EditedOrderDetail detail) {
        return normalizeDetailKey(detail.getOrderDetailId(), detail.getProductId(), detail.getUnitId());
    }

    private String normalizeDetailKey(String orderDetailId, String productId, String unitId) {
        if (orderDetailId != null && !orderDetailId.trim().isEmpty()) {
            return "ID:" + orderDetailId.trim();
        }

        return "PU:" + safe(productId) + ":" + safe(unitId);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void upsertOrderDetail(String orderId, EditedOrderDetail detail) throws SQLException {
        if (detail.getOrderDetailId() != null && !detail.getOrderDetailId().trim().isEmpty()) {
            String updateByIdSql = """
                UPDATE ORDER_DETAILS
                SET product_id = ?,
                    unit_id = ?,
                    quantity = ?,
                    quantity_base = ?,
                    unit_price = ?,
                    is_deleted = 0
                WHERE order_id = ?
                  AND order_detail_id = ?
            """;

            try (PreparedStatement ps = conn.prepareStatement(updateByIdSql)) {
                ps.setString(1, detail.getProductId());
                ps.setString(2, detail.getUnitId());
                ps.setInt(3, detail.getQuantity());
                ps.setInt(4, detail.getQuantityBase());
                ps.setDouble(5, detail.getUnitPrice());
                ps.setString(6, orderId);
                ps.setString(7, detail.getOrderDetailId());

                int updated = ps.executeUpdate();

                if (updated > 0) {
                    return;
                }
            }
        }

        String updateByProductUnitSql = """
            UPDATE ORDER_DETAILS
            SET quantity = ?,
                quantity_base = ?,
                unit_price = ?,
                is_deleted = 0
            WHERE order_id = ?
              AND product_id = ?
              AND NVL(unit_id, 'NULL') = NVL(?, 'NULL')
        """;

        try (PreparedStatement ps = conn.prepareStatement(updateByProductUnitSql)) {
            ps.setInt(1, detail.getQuantity());
            ps.setInt(2, detail.getQuantityBase());
            ps.setDouble(3, detail.getUnitPrice());
            ps.setString(4, orderId);
            ps.setString(5, detail.getProductId());
            ps.setString(6, detail.getUnitId());

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
                unit_id,
                quantity_base,
                unit_price,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)
        """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, detail.getOrderDetailId() != null && !detail.getOrderDetailId().trim().isEmpty()
                    ? detail.getOrderDetailId()
                    : "OD" + System.nanoTime());
            ps.setString(2, orderId);
            ps.setString(3, detail.getProductId());
            ps.setInt(4, detail.getQuantity());
            ps.setString(5, detail.getUnitId());
            ps.setInt(6, detail.getQuantityBase());
            ps.setDouble(7, detail.getUnitPrice());
            ps.executeUpdate();
        }
    }

    private void softDeleteOrderDetail(String orderDetailId) throws SQLException {
        if (orderDetailId == null || orderDetailId.trim().isEmpty()) {
            return;
        }

        String sql = """
            UPDATE ORDER_DETAILS
            SET is_deleted = 1
            WHERE order_detail_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderDetailId);
            ps.executeUpdate();
        }
    }

    private void updateInventoryByDiff(String storeId, String productId, int diffBaseQty) throws SQLException {
        if (diffBaseQty > 0) {
            decreaseInventory(storeId, productId, diffBaseQty);
        } else {
            restoreInventory(storeId, productId, Math.abs(diffBaseQty));
        }
    }

    private void decreaseInventory(String storeId, String productId, int qtyBase) throws SQLException {
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
            ps.setInt(1, qtyBase);
            ps.setString(2, storeId);
            ps.setString(3, productId);
            ps.setInt(4, qtyBase);

            int updated = ps.executeUpdate();

            if (updated <= 0) {
                throw new SQLException("Không đủ tồn kho để cập nhật sản phẩm: " + productId
                        + ". Cần thêm " + qtyBase + " đơn vị gốc.");
            }
        }
    }

    private void restoreInventory(String storeId, String productId, int qtyBase) throws SQLException {
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
                    'Đơn vị gốc',
                    SYSDATE,
                    0
                )
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, storeId);
            ps.setString(2, productId);
            ps.setInt(3, qtyBase);
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

    private static class OldDetailInfo {
        String orderDetailId;
        String productId;
        String unitId;
        int quantity;
        int quantityBase;
    }
}
