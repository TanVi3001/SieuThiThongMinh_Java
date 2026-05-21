package business.sql.sales_order;

import common.db.DatabaseConnection;
import model.order.OrderDetail;
import business.sql.SqlInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrderDetailsSql: Xử lý lưu chi tiết hóa đơn vào Oracle Leader Vĩ: Đã fix
 * Singleton và implement logic Transaction
 */
public class OrderDetailsSql implements SqlInterface<OrderDetail> {

    private static OrderDetailsSql instance;

    private OrderDetailsSql() {
    }

    // Fix Singleton: Đảm bảo chỉ có 1 thực thể duy nhất
    public static OrderDetailsSql getInstance() {
        if (instance == null) {
            instance = new OrderDetailsSql();
        }
        return instance;
    }

    /**
     * Hàm cực kỳ quan trọng: Lưu vào DB bằng Connection dùng chung
     * (Transaction) Dùng cho PaymentService để Rollback nếu có lỗi
     *
     * @param con
     * @param ct
     * @return
     * @throws java.sql.SQLException
     */
    public int insertWithConn(Connection con, OrderDetail ct) throws SQLException {
        // FIX: Thêm cột order_detail_id vào câu lệnh SQL
        String sql = "INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price, unit_id, quantity_base) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            // Tự động tạo một cái mã ID ngẫu nhiên (ví dụ: DET-12345...)
            String randomId = "DET-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            pst.setString(1, randomId); // Điền vào cột order_detail_id
            pst.setString(2, ct.getOrderId());
            pst.setString(3, ct.getProductId());
            pst.setInt(4, ct.getQuantity());
            pst.setDouble(5, ct.getUnitPrice());
            pst.setString(6, ct.getUnitId());
            pst.setInt(7, ct.getQuantityInBaseUnit());

            return pst.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                return insertLegacyWithConn(con, ct);
            }
            throw e;
        }
    }

    private int insertLegacyWithConn(Connection con, OrderDetail ct) throws SQLException {
        String sql = "INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            String randomId = "DET-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            pst.setString(1, randomId);
            pst.setString(2, ct.getOrderId());
            pst.setString(3, ct.getProductId());
            pst.setInt(4, ct.getQuantity());
            pst.setDouble(5, ct.getUnitPrice());
            return pst.executeUpdate();
        }
    }

    @Override
    public int insert(OrderDetail t) {
        int kq = 0;
        try (Connection con = DatabaseConnection.getConnection()) {
            kq = insertWithConn(con, t);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return kq;
    }

    // Lấy tất cả chi tiết của 1 hóa đơn cụ thể
    public ArrayList<OrderDetail> selectByOrderId(String orderId) {
        ArrayList<OrderDetail> ds = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_DETAILS WHERE order_id = ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, orderId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    OrderDetail ct = new OrderDetail(
                            rs.getString("order_id"),
                            rs.getString("product_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            getNullableString(rs, "unit_id"),
                            getNullableInt(rs, "quantity_base")
                    );
                    ds.add(ct);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    public List<Map<String, Object>> selectDetailRowsByOrderId(String orderId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT od.order_detail_id, od.order_id, od.product_id, "
                + "       p.product_name, od.quantity, od.unit_price, od.unit_id, od.quantity_base, "
                + "       (od.quantity * od.unit_price) AS line_total "
                + "FROM ORDER_DETAILS od "
                + "LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE od.order_id = ? AND NVL(od.is_deleted, 0) = 0 "
                + "ORDER BY od.order_detail_id";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, orderId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("order_detail_id", rs.getString("order_detail_id"));
                    row.put("order_id", rs.getString("order_id"));
                    row.put("product_id", rs.getString("product_id"));
                    row.put("product_name", rs.getString("product_name"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unit_price", rs.getDouble("unit_price"));
                    row.put("unit_id", getNullableString(rs, "unit_id"));
                    row.put("quantity_base", getNullableInt(rs, "quantity_base"));
                    row.put("line_total", rs.getDouble("line_total"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                return selectLegacyDetailRowsByOrderId(orderId);
            }
            System.err.println("Loi tai OrderDetailsSql.selectDetailRowsByOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    public List<Map<String, Object>> selectDetailRowsByOrderIdAndStore(String orderId, String storeId) {
        List<Map<String, Object>> rows = new ArrayList<>();

        if (orderId == null || orderId.trim().isEmpty()
                || storeId == null || storeId.trim().isEmpty()) {
            return rows;
        }

        String sql = "SELECT od.order_detail_id, od.order_id, od.product_id, "
                + "       p.product_name, od.quantity, od.unit_price, od.unit_id, od.quantity_base, "
                + "       (od.quantity * od.unit_price) AS line_total "
                + "FROM ORDER_DETAILS od "
                + "JOIN ORDERS o ON o.order_id = od.order_id "
                + "LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE od.order_id = ? "
                + "  AND o.store_id = ? "
                + "  AND NVL(o.is_deleted, 0) = 0 "
                + "  AND NVL(od.is_deleted, 0) = 0 "
                + "ORDER BY od.order_detail_id";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, orderId.trim());
            pst.setString(2, storeId.trim());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("order_detail_id", rs.getString("order_detail_id"));
                    row.put("order_id", rs.getString("order_id"));
                    row.put("product_id", rs.getString("product_id"));
                    row.put("product_name", rs.getString("product_name"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unit_price", rs.getDouble("unit_price"));
                    row.put("unit_id", getNullableString(rs, "unit_id"));
                    row.put("quantity_base", getNullableInt(rs, "quantity_base"));
                    row.put("line_total", rs.getDouble("line_total"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                return selectLegacyDetailRowsByOrderIdAndStore(orderId, storeId);
            }

            System.err.println("Loi tai OrderDetailsSql.selectDetailRowsByOrderIdAndStore: " + e.getMessage());
            e.printStackTrace();
        }

        return rows;
    }

    private List<Map<String, Object>> selectLegacyDetailRowsByOrderIdAndStore(String orderId, String storeId) {
        List<Map<String, Object>> rows = new ArrayList<>();

        if (orderId == null || orderId.trim().isEmpty()
                || storeId == null || storeId.trim().isEmpty()) {
            return rows;
        }

        String sql = "SELECT od.order_detail_id, od.order_id, od.product_id, "
                + "       p.product_name, od.quantity, od.unit_price, "
                + "       (od.quantity * od.unit_price) AS line_total "
                + "FROM ORDER_DETAILS od "
                + "JOIN ORDERS o ON o.order_id = od.order_id "
                + "LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE od.order_id = ? "
                + "  AND o.store_id = ? "
                + "  AND NVL(o.is_deleted, 0) = 0 "
                + "  AND NVL(od.is_deleted, 0) = 0 "
                + "ORDER BY od.order_detail_id";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, orderId.trim());
            pst.setString(2, storeId.trim());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("order_detail_id", rs.getString("order_detail_id"));
                    row.put("order_id", rs.getString("order_id"));
                    row.put("product_id", rs.getString("product_id"));
                    row.put("product_name", rs.getString("product_name"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unit_price", rs.getDouble("unit_price"));
                    row.put("unit_id", null);
                    row.put("quantity_base", rs.getInt("quantity"));
                    row.put("line_total", rs.getDouble("line_total"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            System.err.println("Loi tai OrderDetailsSql.selectLegacyDetailRowsByOrderIdAndStore: " + e.getMessage());
            e.printStackTrace();
        }

        return rows;
    }

    private List<Map<String, Object>> selectLegacyDetailRowsByOrderId(String orderId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT od.order_detail_id, od.order_id, od.product_id, "
                + "       p.product_name, od.quantity, od.unit_price, "
                + "       (od.quantity * od.unit_price) AS line_total "
                + "FROM ORDER_DETAILS od "
                + "LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE od.order_id = ? AND NVL(od.is_deleted, 0) = 0 "
                + "ORDER BY od.order_detail_id";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, orderId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("order_detail_id", rs.getString("order_detail_id"));
                    row.put("order_id", rs.getString("order_id"));
                    row.put("product_id", rs.getString("product_id"));
                    row.put("product_name", rs.getString("product_name"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unit_price", rs.getDouble("unit_price"));
                    row.put("unit_id", null);
                    row.put("quantity_base", rs.getInt("quantity"));
                    row.put("line_total", rs.getDouble("line_total"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi tai OrderDetailsSql.selectLegacyDetailRowsByOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    @Override
    public ArrayList<OrderDetail> selectAll() {
        ArrayList<OrderDetail> ds = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_DETAILS";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                // Vì các trường trong Model là final, ta phải lấy dữ liệu ra biến tạm trước
                String oId = rs.getString("order_id");
                String pId = rs.getString("product_id");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("unit_price");
                String unitId = getNullableString(rs, "unit_id");
                int qtyBase = getNullableInt(rs, "quantity_base");

                // Sau đó truyền tất cả vào Constructor duy nhất của OrderDetail
                OrderDetail ct = new OrderDetail(oId, pId, qty, price, unitId, qtyBase);

                ds.add(ct);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tại OrderDetailsSql.selectAll: " + e.getMessage());
            e.printStackTrace();
        }
        return ds;
    }

    @Override
    public int update(OrderDetail t) {
        return 0;
    }

    @Override
    public int delete(String id) {
        return 0;
    }

    @Override
    public OrderDetail selectById(String id) {
        return null;
    }

    @Override
    public List<OrderDetail> selectByCondition(String condition) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private String getNullableString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private int getNullableInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }
}
