package business.sql.sales_order;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticSql {

    private static StatisticSql instance;

    public StatisticSql() {
    }

    public static StatisticSql getInstance() {
        if (instance == null) {
            instance = new StatisticSql();
        }
        return instance;
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

    private static String cleanStoreId(String storeId) {
        return storeId == null || storeId.trim().isEmpty() ? null : storeId.trim();
    }

    // =========================================================
    // 1. DASHBOARD COUNTERS - GLOBAL + STORE SCOPED
    // =========================================================
    public int getTotalCustomers() {
        return queryInt("SELECT COUNT(*) FROM CUSTOMERS WHERE NVL(is_deleted, 0) = 0");
    }

    /**
     * CUSTOMERS là global. Nhưng nếu dashboard chi nhánh cần số khách của chi
     * nhánh thì phải tính theo khách có phát sinh đơn tại ORDERS.store_id.
     */
    public int getTotalCustomersByStore(String storeId) {
        String sid = cleanStoreId(storeId);

        if (sid == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(DISTINCT customer_id)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND customer_id IS NOT NULL
              AND store_id = ?
        """;

        return queryInt(sql, sid);
    }

    public int getTodayCustomersByStore(String storeId) {
        String sid = cleanStoreId(storeId);

        if (sid == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(DISTINCT customer_id)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND customer_id IS NOT NULL
              AND TRUNC(order_date) = TRUNC(SYSDATE)
              AND store_id = ?
        """;

        return queryInt(sql, sid);
    }

    public int getTotalProducts() {
        return queryInt("SELECT COUNT(*) FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0");
    }

    public int getTotalProductsByStore(String storeId) {
        String sid = cleanStoreId(storeId);

        if (sid == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(DISTINCT i.product_id)
            FROM INVENTORY i
            JOIN PRODUCTS p
                ON p.product_id = i.product_id
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = i.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE i.store_id = ?
              AND NVL(i.is_deleted, 0) = 0
              AND NVL(p.is_deleted, 0) = 0
              AND NVL(sp.is_active, 1) = 1
        """;

        return queryInt(sql, sid);
    }

    public int getTotalOrders() {
        return queryInt("SELECT COUNT(*) FROM ORDERS WHERE NVL(is_deleted, 0) = 0");
    }

    public int getTotalOrdersByStore(String storeId) {
        String sid = cleanStoreId(storeId);

        if (sid == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(*)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND store_id = ?
        """;

        return queryInt(sql, sid);
    }

    public int getTodayOrders() {
        return queryInt("""
            SELECT COUNT(*)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND TRUNC(order_date) = TRUNC(SYSDATE)
        """);
    }

    public int getTodayOrdersByStore(String storeId) {
        String sid = cleanStoreId(storeId);

        if (sid == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(*)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND TRUNC(order_date) = TRUNC(SYSDATE)
              AND store_id = ?
        """;

        return queryInt(sql, sid);
    }

    public double getMonthlyRevenue() {
        return getMonthlyRevenueInternal(null);
    }

    public double getMonthlyRevenueByStore(String storeId) {
        return getMonthlyRevenueInternal(storeId);
    }

    private double getMonthlyRevenueInternal(String storeId) {
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT NVL(SUM(total_amount), 0)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND %s
              AND order_date >= TRUNC(SYSDATE, 'MM')
              AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
        """.formatted(completedCondition("status")));

        if (sid != null) {
            sql.append(" AND store_id = ? ");
            return queryDouble(sql.toString(), sid);
        }

        return queryDouble(sql.toString());
    }

    public double getTodayRevenue() {
        return getTodayRevenueInternal(null);
    }

    public double getTodayRevenueByStore(String storeId) {
        return getTodayRevenueInternal(storeId);
    }

    private double getTodayRevenueInternal(String storeId) {
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT NVL(SUM(total_amount), 0)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND %s
              AND TRUNC(order_date) = TRUNC(SYSDATE)
        """.formatted(completedCondition("status")));

        if (sid != null) {
            sql.append(" AND store_id = ? ");
            return queryDouble(sql.toString(), sid);
        }

        return queryDouble(sql.toString());
    }

    // =========================================================
    // 2. DASHBOARD LISTS / CHARTS - GLOBAL + STORE SCOPED
    // =========================================================
    public List<Map<String, Object>> getBestSellingProducts(int limit) {
        return getBestSellingProductsInternal(limit, null);
    }

    public List<Map<String, Object>> getBestSellingProductsByStore(int limit, String storeId) {
        return getBestSellingProductsInternal(limit, storeId);
    }

    private List<Map<String, Object>> getBestSellingProductsInternal(int limit, String storeId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM (
                SELECT od.product_id,
                       NVL(p.product_name, od.product_id) AS product_name,
                       SUM(NVL(od.quantity_base, od.quantity)) AS total_sold,
                       SUM(NVL(od.quantity_base, od.quantity) * od.unit_price) AS total_revenue
                FROM ORDER_DETAILS od
                JOIN ORDERS o
                    ON od.order_id = o.order_id
                LEFT JOIN PRODUCTS p
                    ON od.product_id = p.product_id
                WHERE NVL(od.is_deleted, 0) = 0
                  AND NVL(o.is_deleted, 0) = 0
                  AND %s
        """.formatted(completedCondition("o.status")));

        if (sid != null) {
            sql.append(" AND o.store_id = ? ");
        }

        sql.append("""
                GROUP BY od.product_id, p.product_name
                ORDER BY total_sold DESC, total_revenue DESC
            )
            WHERE ROWNUM <= ?
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;

            if (sid != null) {
                pst.setString(i++, sid);
            }

            pst.setInt(i, limit <= 0 ? 10 : limit);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("product_id", rs.getString("product_id"));
                    row.put("product_name", rs.getString("product_name"));
                    row.put("total_sold", rs.getInt("total_sold"));
                    row.put("total_revenue", rs.getDouble("total_revenue"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<Map<String, Object>> getRecentOrders(int limit) {
        return getRecentOrdersInternal(limit, null);
    }

    public List<Map<String, Object>> getRecentOrdersByStore(int limit, String storeId) {
        return getRecentOrdersInternal(limit, storeId);
    }

    private List<Map<String, Object>> getRecentOrdersInternal(int limit, String storeId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM (
                SELECT o.order_id,
                       NVL(c.customer_name, 'Khách vãng lai') AS customer_name,
                       o.total_amount,
                       o.status,
                       o.order_date,
                       o.store_id
                FROM ORDERS o
                LEFT JOIN CUSTOMERS c
                    ON o.customer_id = c.customer_id
                WHERE NVL(o.is_deleted, 0) = 0
        """);

        if (sid != null) {
            sql.append(" AND o.store_id = ? ");
        }

        sql.append("""
                ORDER BY o.order_date DESC, o.order_id DESC
            )
            WHERE ROWNUM <= ?
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;

            if (sid != null) {
                pst.setString(i++, sid);
            }

            pst.setInt(i, limit <= 0 ? 10 : limit);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("order_id", rs.getString("order_id"));
                    row.put("customer_name", rs.getString("customer_name"));
                    row.put("total_amount", rs.getDouble("total_amount"));
                    row.put("status", rs.getString("status"));
                    row.put("order_date", rs.getDate("order_date"));
                    row.put("store_id", rs.getString("store_id"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<Map<String, Object>> getLowStockProducts(int limit) {
        return getLowStockProductsInternal(limit, null);
    }

    public List<Map<String, Object>> getLowStockProductsByStore(int limit, String storeId) {
        return getLowStockProductsInternal(limit, storeId);
    }

    private List<Map<String, Object>> getLowStockProductsInternal(int limit, String storeId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM (
                SELECT p.product_name,
                       i.product_id,
                       i.store_id,
                       NVL(i.quantity, 0) AS qty
                FROM PRODUCTS p
                JOIN INVENTORY i
                    ON p.product_id = i.product_id
                LEFT JOIN STORE_PRODUCTS sp
                    ON sp.product_id = i.product_id
                   AND sp.store_id = i.store_id
                   AND NVL(sp.is_deleted, 0) = 0
                WHERE NVL(p.is_deleted, 0) = 0
                  AND NVL(i.is_deleted, 0) = 0
                  AND NVL(sp.is_active, 1) = 1
        """);

        if (sid != null) {
            sql.append(" AND i.store_id = ? ");
        }

        sql.append("""
                ORDER BY i.quantity ASC
            )
            WHERE ROWNUM <= ?
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;

            if (sid != null) {
                pst.setString(i++, sid);
            }

            pst.setInt(i, limit <= 0 ? 10 : limit);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("product_id", rs.getString("product_id"));
                    row.put("store_id", rs.getString("store_id"));
                    row.put("name", rs.getString("product_name"));
                    row.put("qty", rs.getInt("qty"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public Map<String, Double> getRevenueByMonth() throws SQLException {
        return getRevenueByMonthInternal(null);
    }

    public Map<String, Double> getRevenueByMonthByStore(String storeId) throws SQLException {
        return getRevenueByMonthInternal(storeId);
    }

    private Map<String, Double> getRevenueByMonthInternal(String storeId) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT month_year, revenue
            FROM (
                SELECT TO_CHAR(order_date, 'MM/YYYY') AS month_year,
                       TRUNC(order_date, 'MM') AS month_start,
                       SUM(total_amount) AS revenue
                FROM ORDERS
                WHERE NVL(is_deleted, 0) = 0
                  AND %s
        """.formatted(completedCondition("status")));

        if (sid != null) {
            sql.append(" AND store_id = ? ");
        }

        sql.append("""
                GROUP BY TO_CHAR(order_date, 'MM/YYYY'), TRUNC(order_date, 'MM')
                ORDER BY month_start DESC
            )
            WHERE ROWNUM <= 5
            ORDER BY month_year ASC
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (sid != null) {
                ps.setString(1, sid);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("month_year"), rs.getDouble("revenue"));
                }
            }
        }

        return result;
    }

    public Map<String, Integer> getOrdersByDay() throws SQLException {
        return getOrdersByDayInternal(null);
    }

    public Map<String, Integer> getOrdersByDayByStore(String storeId) throws SQLException {
        return getOrdersByDayInternal(storeId);
    }

    private Map<String, Integer> getOrdersByDayInternal(String storeId) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT order_day, order_count
            FROM (
                SELECT TRUNC(order_date) AS order_date_only,
                       TO_CHAR(order_date, 'DD/MM') AS order_day,
                       COUNT(*) AS order_count
                FROM ORDERS
                WHERE NVL(is_deleted, 0) = 0
                  AND order_date >= TRUNC(SYSDATE) - 6
                  AND order_date < TRUNC(SYSDATE) + 1
        """);

        if (sid != null) {
            sql.append(" AND store_id = ? ");
        }

        sql.append("""
                GROUP BY TRUNC(order_date), TO_CHAR(order_date, 'DD/MM')
                ORDER BY order_date_only ASC
            )
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (sid != null) {
                ps.setString(1, sid);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("order_day"), rs.getInt("order_count"));
                }
            }
        }

        return result;
    }

    public Map<String, Integer> getCategoryDistribution() throws SQLException {
        return getCategoryDistributionInternal(null);
    }

    public Map<String, Integer> getCategoryDistributionByStore(String storeId) throws SQLException {
        return getCategoryDistributionInternal(storeId);
    }

    private Map<String, Integer> getCategoryDistributionInternal(String storeId) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT NVL(c.category_name, 'Khác') AS cat,
                   SUM(NVL(i.quantity, 0)) AS cnt
            FROM INVENTORY i
            JOIN PRODUCTS p
                ON p.product_id = i.product_id
            LEFT JOIN CATEGORIES c
                ON p.category_id = c.category_id
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = i.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(i.is_deleted, 0) = 0
              AND NVL(p.is_deleted, 0) = 0
              AND NVL(sp.is_active, 1) = 1
        """);

        if (sid != null) {
            sql.append(" AND i.store_id = ? ");
        }

        sql.append("""
            GROUP BY c.category_name
            HAVING SUM(NVL(i.quantity, 0)) > 0
            ORDER BY cnt DESC
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (sid != null) {
                ps.setString(1, sid);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("cat"), rs.getInt("cnt"));
                }
            }
        }

        return result;
    }

    // =========================================================
    // 3. REPORTS - GLOBAL + STORE SCOPED
    // =========================================================
    public List<Object[]> getRevenueReport(java.util.Date fromDate, java.util.Date toDate) {
        return getRevenueReportInternal(fromDate, toDate, null);
    }

    public List<Object[]> getRevenueReportByStore(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        return getRevenueReportInternal(fromDate, toDate, storeId);
    }

    private List<Object[]> getRevenueReportInternal(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        List<Object[]> rows = new ArrayList<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT TO_CHAR(TRUNC(order_date), 'dd/MM/yyyy') AS ngay,
                   COUNT(order_id) AS tong_don,
                   NVL(SUM(total_amount), 0) AS doanh_thu
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND %s
              AND order_date >= ?
              AND order_date < (? + 1)
        """.formatted(completedCondition("status")));

        if (sid != null) {
            sql.append(" AND store_id = ? ");
        }

        sql.append(" GROUP BY TRUNC(order_date) ORDER BY TRUNC(order_date) DESC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;

            pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
            pst.setDate(i++, new java.sql.Date(toDate.getTime()));

            if (sid != null) {
                pst.setString(i++, sid);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("ngay"),
                        rs.getInt("tong_don"),
                        rs.getDouble("doanh_thu")
                    });
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<Object[]> getProductReport(java.util.Date fromDate, java.util.Date toDate) {
        return getProductReportInternal(fromDate, toDate, null);
    }

    public List<Object[]> getProductReportByStore(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        return getProductReportInternal(fromDate, toDate, storeId);
    }

    private List<Object[]> getProductReportInternal(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        List<Object[]> rows = new ArrayList<>();
        String sid = cleanStoreId(storeId);

        StringBuilder sql = new StringBuilder("""
            SELECT p.product_id,
                   p.product_name,
                   NVL(sold.total_qty, 0) AS qty_sold,
                   NVL(sold.total_revenue, 0) AS revenue,
                   NVL(i.quantity, 0) AS current_stock
            FROM PRODUCTS p
        """);

        if (sid != null) {
            sql.append("""
                LEFT JOIN INVENTORY i
                    ON p.product_id = i.product_id
                   AND i.store_id = ?
                   AND NVL(i.is_deleted, 0) = 0
            """);
        } else {
            sql.append("""
                LEFT JOIN (
                    SELECT product_id,
                           SUM(NVL(quantity, 0)) AS quantity
                    FROM INVENTORY
                    WHERE NVL(is_deleted, 0) = 0
                    GROUP BY product_id
                ) i
                    ON p.product_id = i.product_id
            """);
        }

        sql.append("""
            LEFT JOIN (
                SELECT d.product_id,
                       SUM(NVL(d.quantity_base, d.quantity)) AS total_qty,
                       SUM(NVL(d.quantity_base, d.quantity) * d.unit_price) AS total_revenue
                FROM ORDER_DETAILS d
                INNER JOIN ORDERS o
                    ON d.order_id = o.order_id
                WHERE NVL(d.is_deleted, 0) = 0
                  AND NVL(o.is_deleted, 0) = 0
                  AND %s
                  AND o.order_date >= ?
                  AND o.order_date < (? + 1)
        """.formatted(completedCondition("o.status")));

        if (sid != null) {
            sql.append(" AND o.store_id = ? ");
        }

        sql.append("""
                GROUP BY d.product_id
            ) sold
                ON p.product_id = sold.product_id
            WHERE NVL(p.is_deleted, 0) = 0
        """);

        if (sid != null) {
            sql.append(" AND i.product_id IS NOT NULL ");
        }

        sql.append(" ORDER BY qty_sold DESC, revenue DESC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;

            if (sid != null) {
                pst.setString(i++, sid);
            }

            pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
            pst.setDate(i++, new java.sql.Date(toDate.getTime()));

            if (sid != null) {
                pst.setString(i++, sid);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("qty_sold"),
                        rs.getDouble("revenue"),
                        rs.getInt("current_stock")
                    });
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi báo cáo hàng hóa: " + e.getMessage());
            e.printStackTrace();
        }

        return rows;
    }

    public List<Object[]> getEmployeeReport(java.util.Date fromDate, java.util.Date toDate) {
        return getEmployeeReportInternal(fromDate, toDate, null);
    }

    public List<Object[]> getEmployeeReportByStore(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        return getEmployeeReportInternal(fromDate, toDate, storeId);
    }

    private List<Object[]> getEmployeeReportInternal(java.util.Date fromDate, java.util.Date toDate, String storeId) {
    List<Object[]> rows = new ArrayList<>();
    String sid = cleanStoreId(storeId);

    String completed = completedCondition("o.status");
    String cancelled = cancelledCondition("o.status");

    StringBuilder sql = new StringBuilder("""
        SELECT e.employee_id,
               e.employee_name,
               NVL(agg.don_thanh_cong, 0) AS don_thanh_cong,
               NVL(agg.don_huy, 0) AS don_huy,
               NVL(agg.doanh_thu, 0) AS doanh_thu
        FROM EMPLOYEES e
        INNER JOIN ACCOUNTS a
            ON e.employee_id = a.user_id
        LEFT JOIN (
            SELECT NVL(a_ref.user_id, o.employee_id) AS final_emp_id,
                   COUNT(CASE WHEN %s THEN 1 END) AS don_thanh_cong,
                   COUNT(CASE WHEN %s THEN 1 END) AS don_huy,
                   SUM(CASE WHEN %s THEN o.total_amount ELSE 0 END) AS doanh_thu
            FROM ORDERS o
            LEFT JOIN ACCOUNTS a_ref
                ON o.employee_id = a_ref.account_id
            WHERE NVL(o.is_deleted, 0) = 0
              AND o.order_date >= ?
              AND o.order_date < (? + 1)
    """.formatted(completed, cancelled, completed));

    if (sid != null) {
        sql.append(" AND o.store_id = ? ");
    }

    sql.append("""
            GROUP BY NVL(a_ref.user_id, o.employee_id)
        ) agg
            ON e.employee_id = agg.final_emp_id
        WHERE NVL(e.is_deleted, 0) = 0
          AND e.role_id = 'R_STAFF_SALE'
          AND (
                UPPER(NVL(a.status, '')) LIKE '%HOẠT ĐỘNG%'
                OR UPPER(NVL(a.status, '')) LIKE '%HOAT DONG%'
                OR a.status = N'Đã cấp'
              )
    """);

    if (sid != null) {
        sql.append(" AND e.store_id = ? ");
    }

    sql.append(" ORDER BY doanh_thu DESC ");

    try (
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement pst = con.prepareStatement(sql.toString())
    ) {
        int i = 1;

        pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
        pst.setDate(i++, new java.sql.Date(toDate.getTime()));

        if (sid != null) {
            pst.setString(i++, sid); // o.store_id
            pst.setString(i++, sid); // e.store_id
        }

        try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("employee_id"),
                    rs.getString("employee_name"),
                    rs.getInt("don_thanh_cong"),
                    rs.getInt("don_huy"),
                    rs.getDouble("doanh_thu")
                });
            }
        }

    } catch (Exception e) {
        System.err.println("❌ Lỗi báo cáo nhân viên: " + e.getMessage());
        e.printStackTrace();
    }

    return rows;
}

    // =========================================================
    // 4. COMMON QUERY HELPERS
    // =========================================================
    private int queryInt(String sql, Object... params) {
        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            bindParams(pst, params);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private double queryDouble(String sql, Object... params) {
        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            bindParams(pst, params);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void bindParams(PreparedStatement pst, Object... params) throws SQLException {
        if (params == null) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            Object value = params[i];

            if (value instanceof java.sql.Date d) {
                pst.setDate(i + 1, d);
            } else if (value instanceof Integer n) {
                pst.setInt(i + 1, n);
            } else if (value instanceof Long n) {
                pst.setLong(i + 1, n);
            } else if (value instanceof Double n) {
                pst.setDouble(i + 1, n);
            } else {
                pst.setString(i + 1, value == null ? null : String.valueOf(value));
            }
        }
    }
}
