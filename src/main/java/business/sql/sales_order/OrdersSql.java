package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import common.exception.ConcurrentCheckoutException;
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
import business.service.SessionManager;
import business.sql.sales_order.CustomersSql;

public class OrdersSql implements SqlInterface<Order> {

    private static OrdersSql instance;

    private OrdersSql() {
    }

    public static synchronized OrdersSql getInstance() {
        if (instance == null) {
            instance = new OrdersSql();
        }
        return instance;
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String completedCondition(String columnName) {
        return "("
                + "UPPER(NVL(" + columnName + ", '')) = 'COMPLETED' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%HOÀN THÀNH%' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%HOAN THANH%'"
                + ")";
    }

    private static String cancelledCondition(String columnName) {
        return "("
                + "UPPER(NVL(" + columnName + ", '')) = 'CANCELLED' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%HỦY%' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%HUY%'"
                + ")";
    }

    private static String pendingCondition(String columnName) {
        return "("
                + "UPPER(NVL(" + columnName + ", '')) = 'PENDING' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%ĐANG XỬ LÝ%' "
                + "OR UPPER(NVL(" + columnName + ", '')) LIKE '%DANG XU LY%'"
                + ")";
    }

    private static String statusConditionSql(String columnName, String displayStatus) {
        if (displayStatus == null || displayStatus.trim().isEmpty()
                || "Tất cả".equalsIgnoreCase(displayStatus.trim())
                || "Tat ca".equalsIgnoreCase(displayStatus.trim())) {
            return null;
        }

        String s = displayStatus.trim().toLowerCase();

        if (s.contains("hoàn") || s.contains("hoan") || s.equals("completed")) {
            return completedCondition(columnName);
        }

        if (s.contains("hủy") || s.contains("huỷ") || s.contains("huy") || s.equals("cancelled")) {
            return cancelledCondition(columnName);
        }

        if (s.contains("đang") || s.contains("dang") || s.equals("pending")) {
            return pendingCondition(columnName);
        }

        return "UPPER(NVL(" + columnName + ", '')) = UPPER(?)";
    }

    private static void assertWritableStore(String storeId) throws SQLException {
        if (SessionManager.isAdmin()) {
            return;
        }

        String currentStoreId = SessionManager.getCurrentStoreId();

        if (currentStoreId == null || currentStoreId.trim().isEmpty()) {
            throw new SQLException("Tài khoản chưa được phân chi nhánh.");
        }

        if (storeId == null || !currentStoreId.trim().equalsIgnoreCase(storeId.trim())) {
            throw new SQLException("Bạn không có quyền thao tác dữ liệu của chi nhánh khác.");
        }
    }

    public ArrayList<Order> selectAllByStoreAndEmployee(String storeId, String employeeId) {
        ArrayList<Order> list = new ArrayList<>();

        String sid = clean(storeId);
        String eid = clean(employeeId);

        if (sid == null || eid == null) {
            return list;
        }

        String sql = """
        SELECT
            o.*,
            e.EMPLOYEE_NAME
        FROM ORDERS o
        LEFT JOIN EMPLOYEES e
            ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
        WHERE NVL(o.IS_DELETED, 0) = 0
          AND o.STORE_ID = ?
          AND o.EMPLOYEE_ID = ?
        ORDER BY o.ORDER_DATE DESC
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, sid);
            pst.setString(2, eid);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectAllByStoreAndEmployee(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public List<Order> selectByConditionStoreAndEmployee(String condition, String storeId, String employeeId) {
        if (condition == null
                || condition.isBlank()
                || "Tat ca".equalsIgnoreCase(condition)
                || "Tất cả".equalsIgnoreCase(condition)) {
            return selectAllByStoreAndEmployee(storeId, employeeId);
        }

        List<Order> list = new ArrayList<>();

        String sid = clean(storeId);
        String eid = clean(employeeId);

        if (sid == null || eid == null) {
            return list;
        }

        String statusSql = statusConditionSql("o.STATUS", condition);

        StringBuilder sql = new StringBuilder("""
        SELECT
            o.*,
            e.EMPLOYEE_NAME
        FROM ORDERS o
        LEFT JOIN EMPLOYEES e
            ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
        WHERE NVL(o.IS_DELETED, 0) = 0
          AND o.STORE_ID = ?
          AND o.EMPLOYEE_ID = ?
    """);

        if (statusSql != null) {
            sql.append(" AND ").append(statusSql).append(" ");
        }

        sql.append(" ORDER BY o.ORDER_DATE DESC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;
            pst.setString(i++, sid);
            pst.setString(i++, eid);

            if (statusSql != null && statusSql.contains("UPPER(?)")) {
                pst.setString(i++, condition);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectByConditionStoreAndEmployee(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public List<Order> findByDateRangeStoreAndEmployee(Date fromDate, Date toDate, String storeId, String employeeId) {
        List<Order> list = new ArrayList<>();

        String sid = clean(storeId);
        String eid = clean(employeeId);

        if (sid == null || eid == null) {
            return list;
        }

        String sql = """
        SELECT
            o.*,
            e.EMPLOYEE_NAME
        FROM ORDERS o
        LEFT JOIN EMPLOYEES e
            ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
        WHERE NVL(o.IS_DELETED, 0) = 0
          AND o.STORE_ID = ?
          AND o.EMPLOYEE_ID = ?
          AND o.ORDER_DATE >= ?
          AND o.ORDER_DATE < (? + 1)
        ORDER BY o.ORDER_DATE DESC
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, sid);
            pst.setString(2, eid);
            pst.setDate(3, fromDate);
            pst.setDate(4, toDate);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.findByDateRangeStoreAndEmployee(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public Order selectByIdInStoreAndEmployee(String id, String storeId, String employeeId) {
        String sid = clean(storeId);
        String eid = clean(employeeId);

        if (id == null || id.trim().isEmpty() || sid == null || eid == null) {
            return null;
        }

        String sql = """
        SELECT
            o.*,
            e.EMPLOYEE_NAME
        FROM ORDERS o
        LEFT JOIN EMPLOYEES e
            ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
        WHERE o.ORDER_ID = ?
          AND o.STORE_ID = ?
          AND o.EMPLOYEE_ID = ?
          AND NVL(o.IS_DELETED, 0) = 0
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            pst.setString(2, sid);
            pst.setString(3, eid);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectByIdInStoreAndEmployee(): " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

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
                STORE_ID,
                IS_DELETED
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, order.getOrderId());
            pst.setString(2, normalizeGuestCustomer(order.getCustomerId()));
            pst.setString(3, order.getEmployeeId());
            pst.setString(4, order.getPaymentMethodId());
            pst.setDate(5, order.getOrderDate() != null ? order.getOrderDate() : new Date(System.currentTimeMillis()));
            pst.setDouble(6, order.getTotalAmount());
            pst.setString(7, order.getStatus());
            pst.setString(8, order.getNote());
            pst.setString(9, order.getStoreId());
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
                NOTE = ?,
                STORE_ID = ?
            WHERE ORDER_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, normalizeGuestCustomer(order.getCustomerId()));
            pst.setString(2, order.getEmployeeId());
            pst.setString(3, order.getPaymentMethodId());
            pst.setDate(4, order.getOrderDate() != null ? order.getOrderDate() : new Date(System.currentTimeMillis()));
            pst.setDouble(5, order.getTotalAmount());
            pst.setString(6, order.getStatus());
            pst.setString(7, order.getNote());
            pst.setString(8, order.getStoreId());
            pst.setString(9, order.getOrderId());

            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.update(): " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public int updateStatus(String orderId, String status) {
        String sql = """
            UPDATE ORDERS
            SET STATUS = ?
            WHERE ORDER_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setString(2, orderId);
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.updateStatus(): " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public int updateStatusInStore(String orderId, String status, String storeId) {
        String sql = """
            UPDATE ORDERS
            SET STATUS = ?
            WHERE ORDER_ID = ?
              AND STORE_ID = ?
              AND NVL(IS_DELETED, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setString(2, orderId);
            pst.setString(3, storeId);
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.updateStatusInStore(): " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public int updateStatusWithConn(Connection con, String orderId, String status) throws SQLException {
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

    @Override
    public int delete(String id) {
        String sql = """
            UPDATE ORDERS
            SET IS_DELETED = 1
            WHERE ORDER_ID = ?
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.delete(): " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    public Order selectByIdInStore(String id, String storeId) {
        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE o.ORDER_ID = ?
              AND o.STORE_ID = ?
              AND NVL(o.IS_DELETED, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            pst.setString(2, storeId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectByIdInStore(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectAll(): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Order> selectAllByStoreId(String storeId) {
        ArrayList<Order> list = new ArrayList<>();
        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
              AND o.STORE_ID = ?
            ORDER BY o.ORDER_DATE DESC
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, storeId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectAllByStoreId(): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Order> selectAll(String currentUserRole, String currentEmployeeId) {
        return selectAll(currentUserRole, currentEmployeeId, null);
    }

    public ArrayList<Order> selectAll(String currentUserRole, String currentEmployeeId, String currentStoreId) {
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

        boolean isStaffSale = "R_STAFF_SALE".equalsIgnoreCase(currentUserRole);
        boolean isStoreManager = "R_STORE_MNG".equalsIgnoreCase(currentUserRole);

        if (isStaffSale) {
            sql.append(" AND o.STORE_ID = ? ");
            sql.append(" AND o.EMPLOYEE_ID = ? ");
        } else if (isStoreManager) {
            sql.append(" AND o.STORE_ID = ? ");
        }

        sql.append(" ORDER BY o.ORDER_DATE DESC ");

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int p = 1;
            if (isStaffSale) {
                pst.setString(p++, currentStoreId);
                pst.setString(p++, currentEmployeeId);
            } else if (isStoreManager) {
                pst.setString(p++, currentStoreId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectAll(role, store): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    public List<Order> selectByConditionAndStore(String condition, String storeId) {
        if (condition == null
                || condition.isBlank()
                || "Tat ca".equalsIgnoreCase(condition)
                || "Tất cả".equalsIgnoreCase(condition)) {
            return selectAllByStoreId(storeId);
        }

        List<Order> list = new ArrayList<>();
        String sid = clean(storeId);

        if (sid == null) {
            return list;
        }

        String statusSql = statusConditionSql("o.STATUS", condition);

        StringBuilder sql = new StringBuilder("""
        SELECT
            o.*,
            e.EMPLOYEE_NAME
        FROM ORDERS o
        LEFT JOIN EMPLOYEES e
            ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
        WHERE NVL(o.IS_DELETED, 0) = 0
          AND o.STORE_ID = ?
    """);

        if (statusSql != null) {
            sql.append(" AND ").append(statusSql).append(" ");
        }

        sql.append(" ORDER BY o.ORDER_DATE DESC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;
            pst.setString(i++, sid);

            if (statusSql != null && statusSql.contains("UPPER(?)")) {
                pst.setString(i++, condition);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.selectByConditionAndStore(): " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

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
              AND o.ORDER_DATE >= ?
              AND o.ORDER_DATE < (? + 1)
            ORDER BY o.ORDER_DATE DESC
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    public List<Order> findByDateRangeAndStore(Date fromDate, Date toDate, String storeId) {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT
                o.*,
                e.EMPLOYEE_NAME
            FROM ORDERS o
            LEFT JOIN EMPLOYEES e
                ON o.EMPLOYEE_ID = e.EMPLOYEE_ID
            WHERE NVL(o.IS_DELETED, 0) = 0
              AND o.STORE_ID = ?
              AND o.ORDER_DATE >= ?
              AND o.ORDER_DATE < (? + 1)
            ORDER BY o.ORDER_DATE DESC
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, storeId);
            pst.setDate(2, fromDate);
            pst.setDate(3, toDate);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ OrdersSql.findByDateRangeAndStore(): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getString("ORDER_ID"));
        order.setCustomerId(rs.getString("CUSTOMER_ID"));
        order.setEmployeeId(rs.getString("EMPLOYEE_ID"));

        try {
            order.setStoreId(rs.getString("STORE_ID"));
        } catch (Exception ignored) {
        }

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

        try {
            String empName = rs.getString("EMPLOYEE_NAME");
            if (empName != null && !empName.isBlank()) {
                order.setEmployeeName(empName);
            }
        } catch (Exception ignored) {
        }

        return order;
    }

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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    public String generateNextOrderId() {
        String prefix = "HD" + new SimpleDateFormat("yyMM").format(new java.util.Date()) + "_";
        String sql = """
            SELECT MAX(ORDER_ID)
            FROM ORDERS
            WHERE ORDER_ID LIKE ?
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String maxId = rs.getString(1);
                    if (maxId != null && !maxId.isBlank()) {
                        int nextNumber = Integer.parseInt(maxId.substring(maxId.lastIndexOf("_") + 1)) + 1;
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

    public boolean processCheckoutSecure(Order order, List<OrderDetail> details)
            throws ConcurrentCheckoutException {
        String insertOrderSql = """
            INSERT INTO ORDERS (
                order_id,
                employee_id,
                customer_id,
                payment_method_id,
                total_amount,
                status,
                order_date,
                note,
                store_id,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, 'COMPLETED', SYSDATE, ?, ?, 0)
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

        String updateStockSql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?,
                last_updated = SYSDATE
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

            try (PreparedStatement psOrder = con.prepareStatement(insertOrderSql); PreparedStatement psDetail = con.prepareStatement(insertDetailSql); PreparedStatement psUpdateStock = con.prepareStatement(updateStockSql); PreparedStatement psCheckStock = con.prepareStatement(checkStockSql)) {

                String storeId = requireStoreId(order);
                assertWritableStore(storeId);
                order.setStoreId(storeId);
                psOrder.setString(1, order.getOrderId());
                psOrder.setString(2, order.getEmployeeId());
                psOrder.setString(3, normalizeGuestCustomer(order.getCustomerId()));
                psOrder.setString(4, order.getPaymentMethodId());
                psOrder.setDouble(5, order.getTotalAmount());
                psOrder.setString(6, order.getNote());
                psOrder.setString(7, storeId);
                psOrder.executeUpdate();

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

    private String normalizeGuestCustomer(String customerId) {
        if (customerId == null || customerId.trim().isEmpty() || "Khách vãng lai".equalsIgnoreCase(customerId.trim())) {
            return null;
        }
        return customerId.trim();
    }

    private String requireStoreId(Order order) throws SQLException {
        String storeId = order.getStoreId();
        if (storeId == null || storeId.trim().isEmpty()) {
            throw new SQLException("Order store_id is required.");
        }
        return storeId.trim();
    }
}
