package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.order.Order;
import model.order.OrderDetail;
import common.exception.ConcurrentCheckoutException;

public class OrdersSql implements SqlInterface<Order> {

    // =========================================================
    // SINGLETON
    // =========================================================
    private static OrdersSql instance;

    private OrdersSql() {
    }

    public static synchronized OrdersSql getInstance() {
        if (instance == null) {
            instance = new OrdersSql();
        }
        return instance;
    }

    // =========================================================
    // INSERT
    // =========================================================
    public int insertWithConn(Connection con, Order order) throws SQLException {

        String sql = """
            INSERT INTO ORDERS
            (
                ORDER_ID,
                CUSTOMER_ID,
                EMPLOYEE_ID,
                PAYMENT_METHOD_ID,
                ORDER_DATE,
                TOTAL_AMOUNT,
                STATUS,
                NOTE,
                IS_DELETED
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
        """;

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
    public int insert(Order order) {

        try (Connection con = DatabaseConnection.getConnection()) {

            return insertWithConn(con, order);

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.insert(): " + e.getMessage());
            e.printStackTrace();

            return 0;
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================
    @Override
    public int update(Order order) {

        String sql = """
            UPDATE ORDERS
            SET
                CUSTOMER_ID = ?,
                EMPLOYEE_ID = ?,
                PAYMENT_METHOD_ID = ?,
                ORDER_DATE = ?,
                TOTAL_AMOUNT = ?,
                STATUS = ?,
                NOTE = ?
            WHERE ORDER_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, order.getCustomerId());
            pst.setString(2, order.getEmployeeId());
            pst.setString(3, order.getPaymentMethodId());
            pst.setDate(4, order.getOrderDate());
            pst.setDouble(5, order.getTotalAmount());
            pst.setString(6, order.getStatus());
            pst.setString(7, order.getNote());
            pst.setString(8, order.getOrderId());

            return pst.executeUpdate();

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.update(): " + e.getMessage());
            e.printStackTrace();

            return 0;
        }
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================
    public int updateStatus(String orderId, String status) {

        String sql = """
            UPDATE ORDERS
            SET STATUS = ?
            WHERE ORDER_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, status);
            pst.setString(2, orderId);

            return pst.executeUpdate();

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.updateStatus(): " + e.getMessage());
            e.printStackTrace();

            return 0;
        }
    }

    public int updateStatusWithConn(
            Connection con,
            String orderId,
            String status
    ) throws SQLException {

        String sql = """
            UPDATE ORDERS
            SET STATUS = ?
            WHERE ORDER_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, status);
            pst.setString(2, orderId);

            return pst.executeUpdate();
        }
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================
    @Override
    public int delete(String id) {

        String sql = """
            UPDATE ORDERS
            SET IS_DELETED = 1
            WHERE ORDER_ID = ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, id);

            return pst.executeUpdate();

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.delete(): " + e.getMessage());
            e.printStackTrace();

            return 0;
        }
    }

    // =========================================================
    // SELECT BY ID
    // =========================================================
    @Override
    public Order selectById(String id) {

        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE o.ORDER_ID = ?
              AND NVL(o.IS_DELETED, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, id);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    return mapOrder(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.selectById(): " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // SELECT ALL
    // =========================================================
    @Override
    public ArrayList<Order> selectAll() {

        ArrayList<Order> list = new ArrayList<>();

        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
            ORDER BY o.ORDER_DATE DESC
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(mapOrder(rs));
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.selectAll(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // SELECT ALL WITH ROLE FILTER
    // =========================================================
    public ArrayList<Order> selectAll(
            String currentUserRole,
            String currentEmployeeId
    ) {

        ArrayList<Order> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
        """);

        // Nhân viên bán hàng chỉ xem bill của mình
        if ("R_STAFF_SALE".equalsIgnoreCase(currentUserRole)) {
            sql.append(" AND o.EMPLOYEE_ID = ? ");
        }

        sql.append(" ORDER BY o.ORDER_DATE DESC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {

            if ("R_STAFF_SALE".equalsIgnoreCase(currentUserRole)) {
                pst.setString(1, currentEmployeeId);
            }

            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.selectAll(role): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // SELECT BY CONDITION
    // =========================================================
    @Override
    public List<Order> selectByCondition(String condition) {

        if (condition == null
                || condition.isBlank()
                || "Tat ca".equalsIgnoreCase(condition)
                || "Tất cả".equalsIgnoreCase(condition)) {

            return selectAll();
        }

        List<Order> list = new ArrayList<>();

        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
              AND UPPER(o.STATUS) = UPPER(?)
            ORDER BY o.ORDER_DATE DESC
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, condition);

            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.selectByCondition(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // MAP RESULTSET -> ORDER
    // =========================================================
    private Order mapOrder(ResultSet rs) throws SQLException {

        Order order = new Order();

        order.setOrderId(rs.getString("ORDER_ID"));
        order.setCustomerId(rs.getString("CUSTOMER_ID"));
        order.setEmployeeId(rs.getString("EMPLOYEE_ID"));

        try {
            order.setPaymentMethodId(rs.getString("PAYMENT_METHOD_ID"));
        } catch (Exception ignored) {
        }

        order.setOrderDate(rs.getDate("ORDER_DATE"));
        order.setTotalAmount(rs.getDouble("TOTAL_AMOUNT"));
        order.setStatus(rs.getString("STATUS"));

        try {
            order.setNote(rs.getString("NOTE"));
        } catch (Exception ignored) {
        }

        try {
            order.setDeleted(rs.getInt("IS_DELETED") == 1);
        } catch (Exception ignored) {
        }

        // TEMP: lưu tên nhân viên vào note phụ nếu cần
        try {
            String empName = rs.getString("EMPLOYEE_NAME");

            if (empName != null && !empName.isBlank()) {
                order.setEmployeeName(empName);
            }

        } catch (Exception ignored) {
        }

        return order;
    }

    // =========================================================
    // CHI TIẾT HÓA ĐƠN
    // =========================================================
    public List<Object[]> getOrderDetailsByOrderId(String orderId) {

        List<Object[]> list = new ArrayList<>();

        String sql = """
            SELECT
                od.PRODUCT_ID,
                p.PRODUCT_NAME,
                od.QUANTITY,
                od.UNIT_PRICE,
                (od.QUANTITY * od.UNIT_PRICE) AS TOTAL_PRICE
            FROM ORDER_DETAILS od
            JOIN PRODUCTS p
                ON od.PRODUCT_ID = p.PRODUCT_ID
            WHERE od.ORDER_ID = ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, orderId);

            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {

                    Object[] row = new Object[5];

                    row[0] = rs.getString("PRODUCT_ID");
                    row[1] = rs.getString("PRODUCT_NAME");
                    row[2] = rs.getInt("QUANTITY");
                    row[3] = rs.getBigDecimal("UNIT_PRICE");
                    row[4] = rs.getBigDecimal("TOTAL_PRICE");

                    list.add(row);
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.getOrderDetailsByOrderId(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // GENERATE ORDER ID
    // VD: HD2605_001
    // =========================================================
    public String generateNextOrderId() {

        String prefix = "HD"
                + new SimpleDateFormat("yyMM").format(new java.util.Date())
                + "_";

        String sql = """
            SELECT MAX(ORDER_ID)
            FROM ORDERS
            WHERE ORDER_ID LIKE ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, prefix + "%");

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {

                    String maxId = rs.getString(1);

                    if (maxId != null && !maxId.isBlank()) {

                        int nextNumber = Integer.parseInt(
                                maxId.substring(maxId.lastIndexOf("_") + 1)
                        ) + 1;

                        return prefix + String.format("%03d", nextNumber);
                    }
                }
            }

        } catch (Exception e) {

            System.err.println("❌ OrdersSql.generateNextOrderId(): " + e.getMessage());
            e.printStackTrace();
        }

        return prefix + "001";
    }

    // =========================================================
    // SEARCH BY DATE RANGE
    // =========================================================
    public List<Order> findByDateRange(Date fromDate, Date toDate) {

        List<Order> list = new ArrayList<>();

        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
              AND o.ORDER_DATE BETWEEN ? AND ?
            ORDER BY o.ORDER_DATE DESC
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, fromDate);
            pst.setDate(2, toDate);

            try (ResultSet rs = pst.executeQuery()) {

                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ OrdersSql.findByDateRange(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // Nằm trong OrderService.java hoặc OrdersSql.java
    public boolean processCheckoutSecure(Order order, List<OrderDetail> details) throws ConcurrentCheckoutException {
        String insertOrderSql = "INSERT INTO ORDERS (order_id, employee_id, customer_id, total_amount, status, order_date) VALUES (?, ?, ?, ?, 'COMPLETED', SYSDATE)";
        String insertDetailSql = "INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
        // 🌟 ATOMIC UPDATE TRÊN BẢNG INVENTORY: Tuyệt đối không để âm kho
        String updateStockSql = "UPDATE INVENTORY SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?";

        // Dùng để lấy số lượng thực tế trả về cho user nếu thất bại
        String checkStockSql = "SELECT quantity FROM INVENTORY WHERE product_id = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false); // Bắt đầu Transaction

            try (PreparedStatement psOrder = con.prepareStatement(insertOrderSql); PreparedStatement psDetail = con.prepareStatement(insertDetailSql); PreparedStatement psUpdateStock = con.prepareStatement(updateStockSql); PreparedStatement psCheckStock = con.prepareStatement(checkStockSql)) {

                // 1. Tạo hóa đơn
                psOrder.setString(1, order.getOrderId());
                // ... set các tham số khác cho Order ...
                psOrder.executeUpdate();

                Map<String, Integer> failedItems = new HashMap<>();

                // 2. Xử lý từng sản phẩm trong giỏ
                for (OrderDetail d : details) {
                    // Trừ kho an toàn
                    psUpdateStock.setInt(1, d.getQuantity());
                    psUpdateStock.setString(2, d.getProductId());
                    psUpdateStock.setInt(3, d.getQuantity()); // ĐK: Tồn kho phải >= SL Yêu cầu

                    int updatedRows = psUpdateStock.executeUpdate();

                    if (updatedRows == 0) {
                        // ❌ THẤT BẠI: Sản phẩm này đã bị ai đó mua hết hoặc không đủ
                        // Truy vấn tồn thực tế hiện tại để báo cho user
                        psCheckStock.setString(1, d.getProductId());
                        try (ResultSet rs = psCheckStock.executeQuery()) {
                            if (rs.next()) {
                                failedItems.put(d.getProductId(), rs.getInt("quantity"));
                            } else {
                                failedItems.put(d.getProductId(), 0); // Không tìm thấy kho
                            }
                        }
                    } else {
                        // ✔ THÀNH CÔNG: Thêm vào Order Details
                        psDetail.setString(1, "OD" + System.nanoTime()); // Tạo ID tự động
                        psDetail.setString(2, order.getOrderId());
                        psDetail.setString(3, d.getProductId());
                        psDetail.setInt(4, d.getQuantity());
                        psDetail.setDouble(5, d.getUnitPrice());
                        psDetail.addBatch();
                    }
                }

                // 3. Nếu có bất kỳ item nào lỗi -> HỦY BỎ TOÀN BỘ
                if (!failedItems.isEmpty()) {
                    con.rollback(); // Khôi phục lại trạng thái kho ban đầu
                    throw new ConcurrentCheckoutException(failedItems); // Ném lỗi cho UI bắt
                }

                // Nếu mọi thứ OK -> Lưu Database
                psDetail.executeBatch();
                con.commit();
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
}
