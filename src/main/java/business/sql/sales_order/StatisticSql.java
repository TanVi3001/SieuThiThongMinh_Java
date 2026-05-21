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

    public int getTotalCustomers() {
        return queryInt("SELECT COUNT(*) FROM CUSTOMERS WHERE NVL(is_deleted, 0) = 0");
    }

    public int getTotalProducts() {
        return queryInt("SELECT COUNT(*) FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0");
    }

    public int getTotalOrders() {
        return queryInt("SELECT COUNT(*) FROM ORDERS WHERE NVL(is_deleted, 0) = 0");
    }

    public int getTodayOrders() {
        return queryInt("SELECT COUNT(*) FROM ORDERS WHERE NVL(is_deleted, 0) = 0 AND TRUNC(order_date) = TRUNC(SYSDATE)");
    }

    public double getMonthlyRevenue() {
        String sql = """
            SELECT NVL(SUM(total_amount), 0)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND %s
              AND order_date >= TRUNC(SYSDATE, 'MM')
              AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
        """.formatted(completedCondition("status"));

        return queryDouble(sql);
    }

    public double getTodayRevenue() {
        String sql = """
            SELECT NVL(SUM(total_amount), 0)
            FROM ORDERS
            WHERE NVL(is_deleted, 0) = 0
              AND %s
              AND TRUNC(order_date) = TRUNC(SYSDATE)
        """.formatted(completedCondition("status"));

        return queryDouble(sql);
    }

    public List<Map<String, Object>> getBestSellingProducts(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();

        String sql = """
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
                GROUP BY od.product_id, p.product_name
                ORDER BY total_sold DESC, total_revenue DESC
            )
            WHERE ROWNUM <= ?
        """.formatted(completedCondition("o.status"));

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, limit);
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
        List<Map<String, Object>> rows = new ArrayList<>();

        String sql = """
            SELECT *
            FROM (
                SELECT o.order_id,
                       NVL(c.customer_name, 'Khách vãng lai') AS customer_name,
                       o.total_amount,
                       o.status,
                       o.order_date
                FROM ORDERS o
                LEFT JOIN CUSTOMERS c
                    ON o.customer_id = c.customer_id
                WHERE NVL(o.is_deleted, 0) = 0
                ORDER BY o.order_date DESC, o.order_id DESC
            )
            WHERE ROWNUM <= ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("order_id", rs.getString("order_id"));
                    row.put("customer_name", rs.getString("customer_name"));
                    row.put("total_amount", rs.getDouble("total_amount"));
                    row.put("status", rs.getString("status"));
                    row.put("order_date", rs.getDate("order_date"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<Map<String, Object>> getLowStockProducts(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();

        String sql = """
            SELECT *
            FROM (
                SELECT p.product_name,
                       NVL(i.quantity, 0) AS qty
                FROM PRODUCTS p
                JOIN INVENTORY i
                    ON p.product_id = i.product_id
                WHERE NVL(p.is_deleted, 0) = 0
                  AND NVL(i.is_deleted, 0) = 0
                ORDER BY i.quantity ASC
            )
            WHERE ROWNUM <= ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
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
        Map<String, Double> result = new LinkedHashMap<>();

        String sql = """
            SELECT *
            FROM (
                SELECT TO_CHAR(order_date, 'MM/YYYY') AS month_year,
                       MAX(order_date) AS max_date,
                       SUM(total_amount) AS revenue
                FROM ORDERS
                WHERE NVL(is_deleted, 0) = 0
                  AND %s
                GROUP BY TO_CHAR(order_date, 'MM/YYYY')
                ORDER BY max_date DESC
            )
            WHERE ROWNUM <= 5
        """.formatted(completedCondition("status"));

        List<String> keys = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                keys.add(rs.getString("month_year"));
                values.add(rs.getDouble("revenue"));
            }
        }

        for (int i = keys.size() - 1; i >= 0; i--) {
            result.put(keys.get(i), values.get(i));
        }

        return result;
    }

    public Map<String, Integer> getOrdersByDay() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        String sql = """
            SELECT *
            FROM (
                SELECT TO_CHAR(order_date, 'DD/MM') AS order_day,
                       MAX(order_date) AS max_date,
                       COUNT(*) AS order_count
                FROM ORDERS
                WHERE NVL(is_deleted, 0) = 0
                GROUP BY TO_CHAR(order_date, 'DD/MM')
                ORDER BY max_date DESC
            )
            WHERE ROWNUM <= 7
        """;

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                keys.add(rs.getString("order_day"));
                values.add(rs.getInt("order_count"));
            }
        }

        for (int i = keys.size() - 1; i >= 0; i--) {
            result.put(keys.get(i), values.get(i));
        }

        return result;
    }

    public Map<String, Integer> getCategoryDistribution() throws SQLException {
        Map<String, Integer> result = new HashMap<>();

        String sql = """
            SELECT NVL(c.category_name, 'Khác') AS cat,
                   COUNT(*) AS cnt
            FROM PRODUCTS p
            LEFT JOIN CATEGORIES c
                ON p.category_id = c.category_id
            WHERE NVL(p.is_deleted, 0) = 0
            GROUP BY c.category_name
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("cat"), rs.getInt("cnt"));
            }
        }

        return result;
    }

    public List<Object[]> getRevenueReport(java.util.Date fromDate, java.util.Date toDate) {
        return getRevenueReportInternal(fromDate, toDate, null);
    }

    public List<Object[]> getRevenueReportByStore(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        return getRevenueReportInternal(fromDate, toDate, storeId);
    }

    private List<Object[]> getRevenueReportInternal(java.util.Date fromDate, java.util.Date toDate, String storeId) {
        List<Object[]> rows = new ArrayList<>();

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

        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND store_id = ? ");
        }

        sql.append(" GROUP BY TRUNC(order_date) ORDER BY TRUNC(order_date) DESC ");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;
            pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
            pst.setDate(i++, new java.sql.Date(toDate.getTime()));
            if (storeId != null && !storeId.isBlank()) {
                pst.setString(i++, storeId.trim());
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

        StringBuilder sql = new StringBuilder("""
            SELECT p.product_id,
                   p.product_name,
                   NVL(sold.total_qty, 0) AS qty_sold,
                   NVL(sold.total_revenue, 0) AS revenue,
                   NVL(i.quantity, 0) AS current_stock
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON p.product_id = i.product_id
               AND NVL(i.is_deleted, 0) = 0
            LEFT JOIN (
                SELECT d.product_id,
                       SUM(d.quantity) AS total_qty,
                       SUM(d.quantity * d.unit_price) AS total_revenue
                FROM ORDER_DETAILS d
                INNER JOIN ORDERS o
                    ON d.order_id = o.order_id
                WHERE NVL(d.is_deleted, 0) = 0
                  AND NVL(o.is_deleted, 0) = 0
                  AND %s
                  AND o.order_date >= ?
                  AND o.order_date < (? + 1)
        """.formatted(completedCondition("o.status")));

        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND o.store_id = ? ");
        }

        sql.append("""
                GROUP BY d.product_id
            ) sold ON p.product_id = sold.product_id
            WHERE NVL(p.is_deleted, 0) = 0
        """);

        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND i.store_id = ? ");
        }

        sql.append(" ORDER BY qty_sold DESC, revenue DESC ");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;
            pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
            pst.setDate(i++, new java.sql.Date(toDate.getTime()));
            if (storeId != null && !storeId.isBlank()) {
                pst.setString(i++, storeId.trim());
                pst.setString(i++, storeId.trim());
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

        if (storeId != null && !storeId.isBlank()) {
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

        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND e.store_id = ? ");
        }

        sql.append(" ORDER BY doanh_thu DESC ");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql.toString())) {
            int i = 1;
            pst.setDate(i++, new java.sql.Date(fromDate.getTime()));
            pst.setDate(i++, new java.sql.Date(toDate.getTime()));
            if (storeId != null && !storeId.isBlank()) {
                pst.setString(i++, storeId.trim());
                pst.setString(i++, storeId.trim());
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

    private int queryInt(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private double queryDouble(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
