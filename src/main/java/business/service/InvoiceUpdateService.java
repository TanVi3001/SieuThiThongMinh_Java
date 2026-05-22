package business.service.invoice;

import java.sql.*;
import java.util.*;

public class InvoiceUpdateService {

    private final Connection conn;

    public InvoiceUpdateService(Connection conn) {
        this.conn = conn;
    }

    public static class EditedOrderDetail {

        public String productId;
        public int quantity;
        public double unitPrice;

        public EditedOrderDetail(String productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public double getLineTotal() {
            return quantity * unitPrice;
        }
    }

    public void saveInvoiceChanges(String orderId, List<EditedOrderDetail> newDetails) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            String customerId = getCustomerIdByOrder(orderId);

            Map<String, Integer> oldQuantities = getOldQuantities(orderId);

            for (EditedOrderDetail detail : newDetails) {
                if (detail.quantity <= 0) {
                    throw new SQLException("Số lượng sản phẩm phải lớn hơn 0: " + detail.productId);
                }

                int oldQty = oldQuantities.getOrDefault(detail.productId, 0);
                int diff = detail.quantity - oldQty;

                if (diff > 0) {
                    decreaseStock(detail.productId, diff);
                } else if (diff < 0) {
                    increaseStock(detail.productId, -diff);
                }

                updateOrderDetail(orderId, detail);
            }

            double newTotal = calculateNewTotal(newDetails);
            updateOrderTotal(orderId, newTotal);

            if (customerId != null && !customerId.trim().isEmpty() && !customerId.equals("-")) {
                recalculateCustomerSpending(customerId);
                updateCustomerRank(customerId);
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private String getCustomerIdByOrder(String orderId) throws SQLException {
        String sql = """
            SELECT customer_id
            FROM orders
            WHERE order_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("customer_id");
                }
            }
        }

        return null;
    }

    private Map<String, Integer> getOldQuantities(String orderId) throws SQLException {
        Map<String, Integer> map = new HashMap<>();

        String sql = """
            SELECT product_id, quantity
            FROM order_details
            WHERE order_id = ?
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

    private void decreaseStock(String productId, int amount) throws SQLException {
        String lockSql = """
            SELECT quantity
            FROM products
            WHERE product_id = ?
            FOR UPDATE
        """;

        int currentStock;

        try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
            ps.setString(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy sản phẩm: " + productId);
                }

                currentStock = rs.getInt("quantity");
            }
        }

        if (currentStock < amount) {
            throw new SQLException(
                    "Không đủ tồn kho cho sản phẩm " + productId
                    + ". Còn: " + currentStock
                    + ", cần thêm: " + amount
            );
        }

        String updateSql = """
            UPDATE products
            SET quantity = quantity - ?
            WHERE product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, amount);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    private void increaseStock(String productId, int amount) throws SQLException {
        String sql = """
            UPDATE products
            SET quantity = quantity + ?
            WHERE product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    private void updateOrderDetail(String orderId, EditedOrderDetail detail) throws SQLException {
        String sql = """
            UPDATE order_details
            SET quantity = ?,
                unit_price = ?,
                total_price = ?
            WHERE order_id = ?
              AND product_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detail.quantity);
            ps.setDouble(2, detail.unitPrice);
            ps.setDouble(3, detail.getLineTotal());
            ps.setString(4, orderId);
            ps.setString(5, detail.productId);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Không tìm thấy chi tiết đơn hàng: " + detail.productId);
            }
        }
    }

    private double calculateNewTotal(List<EditedOrderDetail> details) {
        double total = 0;

        for (EditedOrderDetail detail : details) {
            total += detail.getLineTotal();
        }

        return total;
    }

    private void updateOrderTotal(String orderId, double total) throws SQLException {
        String sql = """
            UPDATE orders
            SET total_amount = ?
            WHERE order_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, total);
            ps.setString(2, orderId);
            ps.executeUpdate();
        }
    }

    private void recalculateCustomerSpending(String customerId) throws SQLException {
        String sql = """
            UPDATE customers c
            SET c.total_spending = (
                SELECT NVL(SUM(o.total_amount), 0)
                FROM orders o
                WHERE o.customer_id = c.customer_id
                  AND NVL(o.is_deleted, 0) = 0
                  AND UPPER(o.status) IN ('HOÀN THÀNH', 'HOAN THANH', 'COMPLETED')
            )
            WHERE c.customer_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }

    private void updateCustomerRank(String customerId) throws SQLException {
        String sql = """
            UPDATE customers
            SET remember_rank =
                CASE
                    WHEN total_spending >= 50000000 THEN 'DIAMOND'
                    WHEN total_spending >= 20000000 THEN 'GOLD'
                    WHEN total_spending >= 5000000 THEN 'SILVER'
                    ELSE 'BRONZE'
                END
            WHERE customer_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }
}
