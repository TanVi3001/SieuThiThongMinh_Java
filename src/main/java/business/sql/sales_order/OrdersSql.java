package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.order.Order;

public class OrdersSql implements SqlInterface<Order> {

    private static OrdersSql instance;

    private OrdersSql() {
    }

    public static OrdersSql getInstance() {
        if (instance == null) {
            instance = new OrdersSql();
        }
        return instance;
    }

    public int insertWithConn(Connection con, Order order) throws SQLException {
        String sql = "INSERT INTO ORDERS (order_id, customer_id, employee_id, payment_method_id, order_date, total_amount, status, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, order.getOrderId());
            pst.setString(2, order.getCustomerId());
            pst.setString(3, order.getEmployeeId());
            pst.setString(4, order.getPaymentMethodId());
            pst.setDate(5, order.getOrderDate());
            pst.setDouble(6, order.getTotalAmount());
            pst.setString(7, order.getStatus());
            pst.setString(8, order.getNote());
            return pst.executeUpdate();
        }
    }

    @Override
    public int insert(Order t) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertWithConn(con, t);
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.insert: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int update(Order t) {
        String sql = "UPDATE ORDERS "
                + "SET customer_id = ?, employee_id = ?, order_date = ?, total_amount = ?, status = ? "
                + "WHERE order_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getCustomerId());
            pst.setString(2, t.getEmployeeId());
            pst.setDate(3, t.getOrderDate());
            pst.setDouble(4, t.getTotalAmount());
            pst.setString(5, t.getStatus());
            pst.setString(6, t.getOrderId());
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.update: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public int updateStatus(String orderId, String status) {
        String sql = "UPDATE ORDERS SET status = ? WHERE order_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setString(2, orderId);
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.updateStatus: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public int updateStatusWithConn(Connection con, String orderId, String status) throws SQLException {
        String sql = "UPDATE ORDERS SET status = ? WHERE order_id = ? AND NVL(is_deleted, 0) = 0";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setString(2, orderId);
            return pst.executeUpdate();
        }
    }

    @Override
    public int delete(String id) {
        String sql = "UPDATE ORDERS SET is_deleted = 1 WHERE order_id = ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.delete: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Order selectById(String id) {
        String sql = "SELECT * FROM ORDERS WHERE order_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.selectById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Order> selectByCondition(String condition) {
        if (condition == null
                || condition.isBlank()
                || "Tat ca".equalsIgnoreCase(condition)
                || "Tất cả".equalsIgnoreCase(condition)) {
            return selectAll();
        }

        ArrayList<Order> list = new ArrayList<>();
        // Tương tự, JOIN thêm bảng EMPLOYEES
        String sql = "SELECT o.*, e.employee_name "
                + "FROM ORDERS o "
                + "LEFT JOIN EMPLOYEES e ON o.employee_id = e.employee_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 AND UPPER(o.status) = UPPER(?) "
                + "ORDER BY o.order_date DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, condition);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.selectByCondition: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ArrayList<Order> selectAll() {
        ArrayList<Order> list = new ArrayList<>();
        // JOIN thêm bảng EMPLOYEES để lấy employee_name
        String sql = "SELECT o.*, e.employee_name "
                + "FROM ORDERS o "
                + "LEFT JOIN EMPLOYEES e ON o.employee_id = e.employee_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 "
                + "ORDER BY o.order_date DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            System.err.println("Loi tai OrdersSql.selectAll: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Order> selectAll(String currentUserRole, String currentEmployeeId) {
        ArrayList<Order> list = new ArrayList<>();

        String sql = "SELECT o.*, e.employee_name "
                + "FROM ORDERS o "
                + "LEFT JOIN EMPLOYEES e ON o.employee_id = e.employee_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 ";

        // NẾU LÀ SALE: Bắt buộc chỉ lấy hóa đơn do chính ID của người đó tạo
        if ("R_STAFF_SALE".equalsIgnoreCase(currentUserRole)) {
            sql += " AND o.employee_id = ? ";
        }

        sql += " ORDER BY o.order_date DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            // Gán param nếu là Sale
            if ("R_STAFF_SALE".equalsIgnoreCase(currentUserRole)) {
                pst.setString(1, currentEmployeeId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        String id = rs.getString("order_id");
        String customerId = rs.getString("customer_id");
        String employeeId = rs.getString("employee_id");
        java.sql.Date date = rs.getDate("order_date");
        double amount = rs.getDouble("total_amount");
        String status = rs.getString("status");
        boolean deleted = rs.getInt("is_deleted") == 1;

        Order order = new Order(id, customerId, employeeId, date, amount, status, deleted);

        // Bắt lỗi an toàn
        try {
            String empName = rs.getString("employee_name");
            order.setNote(empName);
        } catch (Exception e) {
        }

        return order;
    }

    // =========================================================================
    // LẤY DANH SÁCH CHI TIẾT CỦA 1 HÓA ĐƠN DỰA VÀO MÃ ĐƠN (ORDER_ID)
    // =========================================================================
    public List<Object[]> getOrderDetailsByOrderId(String orderId) {
        List<Object[]> list = new ArrayList<>();

        // Query móc nối 2 bảng để lấy Tên sản phẩm và tính Thành tiền
        String sql = "SELECT od.product_id, p.product_name, od.quantity, od.unit_price, "
                + "(od.quantity * od.unit_price) AS total_price "
                + "FROM ORDER_DETAILS od "
                + "JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE od.order_id = ?";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, orderId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[5];
                    row[0] = rs.getString("product_id");
                    row[1] = rs.getString("product_name");
                    row[2] = rs.getInt("quantity");
                    row[3] = rs.getBigDecimal("unit_price"); // Hoặc getDouble tùy data type của ông
                    row[4] = rs.getBigDecimal("total_price");

                    list.add(row);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi Load Chi tiết hóa đơn: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // =========================================================================
    // TẠO MÃ HÓA ĐƠN TỰ ĐỘNG (Ví dụ: HD2605_001)
    // =========================================================================
    public String generateNextOrderId() {
        // Lấy yyMM hiện tại (Ví dụ Tháng 5 năm 2026 -> 2605)
        String prefix = "HD" + new java.text.SimpleDateFormat("yyMM").format(new java.util.Date()) + "_";
        String sql = "SELECT MAX(order_id) FROM ORDERS WHERE order_id LIKE ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    String maxId = rs.getString(1); // VD: HD2605_009
                    // Cắt lấy phần số phía sau dấu _
                    int nextNum = Integer.parseInt(maxId.substring(maxId.lastIndexOf("_") + 1)) + 1;
                    return prefix + String.format("%03d", nextNum); // Format thành 3 chữ số: 010
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prefix + "001"; // Nếu tháng này chưa có đơn nào thì bắt đầu từ 001
    }
}
