package business.sql.sales_order;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticSql {

    private static StatisticSql instance;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    private StatisticSql() {
    }

    public static StatisticSql getInstance() {
        if (instance == null) {
            instance = new StatisticSql();
        }
        return instance;
    }

    public int getTotalCustomers() {
        String sql = "SELECT COUNT(*) FROM CUSTOMERS WHERE NVL(is_deleted, 0) = 0";
        return queryInt(sql);
    }

    public int getTotalProducts() {
        String sql = "SELECT COUNT(*) FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0";
        return queryInt(sql);
    }

    public int getTotalOrders() {
        String sql = "SELECT COUNT(*) FROM ORDERS WHERE NVL(is_deleted, 0) = 0";
        return queryInt(sql);
    }

    public int getTodayOrders() {
        String sql = "SELECT COUNT(*) FROM ORDERS "
                + "WHERE NVL(is_deleted, 0) = 0 AND TRUNC(order_date) = TRUNC(SYSDATE)";
        return queryInt(sql);
    }

    public double getMonthlyRevenue() {
        String sql = "SELECT NVL(SUM(total_amount), 0) FROM ORDERS "
                + "WHERE NVL(is_deleted, 0) = 0 "
                + "AND UPPER(NVL(status, '')) <> 'CANCELLED' "
                + "AND order_date >= TRUNC(SYSDATE, 'MM') "
                + "AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)";
        return queryDouble(sql);
    }

    public List<Map<String, Object>> getBestSellingProducts(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM ("
                + "SELECT od.product_id, NVL(p.product_name, od.product_id) AS product_name, "
                + "       SUM(NVL(od.quantity_base, od.quantity)) AS total_sold, "
                + "       SUM(NVL(od.quantity_base, od.quantity) * od.unit_price) AS total_revenue "
                + "FROM ORDER_DETAILS od "
                + "JOIN ORDERS o ON od.order_id = o.order_id "
                + "LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE NVL(od.is_deleted, 0) = 0 "
                + "AND NVL(o.is_deleted, 0) = 0 "
                + "AND UPPER(NVL(o.status, '')) <> 'CANCELLED' "
                + "GROUP BY od.product_id, p.product_name "
                + "ORDER BY total_sold DESC, total_revenue DESC"
                + ") WHERE ROWNUM <= ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM ("
                + "SELECT o.order_id, NVL(c.customer_name, 'Khách vãng lai') AS customer_name, "
                + "       o.total_amount, o.status, o.order_date "
                + "FROM ORDERS o "
                + "LEFT JOIN CUSTOMERS c ON o.customer_id = c.customer_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 "
                + "ORDER BY o.order_date DESC, o.order_id DESC"
                + ") WHERE ROWNUM <= ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    private int queryInt(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double queryDouble(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =========================================================
    // CÁC HÀM CHO TRẠM ĐIỀU KHIỂN (TONGQUANPANEL) - DATA THẬT
    // =========================================================
    public Map<String, Integer> getDashboardAlerts() throws SQLException {
        Map<String, Integer> alerts = new HashMap<>();
        String sqlLowStock = "SELECT COUNT(*) FROM INVENTORY WHERE quantity < 10 AND NVL(is_deleted, 0) = 0";
        String sqlPendingOrder = "SELECT COUNT(*) FROM ORDERS WHERE status = N'Đang xử lý' AND NVL(is_deleted, 0) = 0";
        String sqlNewCustomer = "SELECT COUNT(*) FROM CUSTOMERS WHERE NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps1 = con.prepareStatement(sqlLowStock); ResultSet rs1 = ps1.executeQuery()) {
                alerts.put("low_stock", rs1.next() ? rs1.getInt(1) : 0);
            }
            try (PreparedStatement ps2 = con.prepareStatement(sqlPendingOrder); ResultSet rs2 = ps2.executeQuery()) {
                alerts.put("pending_orders", rs2.next() ? rs2.getInt(1) : 0);
            }
            try (PreparedStatement ps3 = con.prepareStatement(sqlNewCustomer); ResultSet rs3 = ps3.executeQuery()) {
                alerts.put("new_customers", rs3.next() ? rs3.getInt(1) : 0);
            }
        }
        return alerts;
    }

    public List<Object[]> getPriorityTasks() throws SQLException {
        List<Object[]> tasks = new ArrayList<>();
        String sqlProd = "SELECT 'Hết hàng' AS loai, p.product_id, p.product_name, 'Tồn: ' || i.quantity AS trang_thai "
                + "FROM PRODUCTS p JOIN INVENTORY i ON p.product_id = i.product_id "
                + "WHERE i.quantity < 10 AND NVL(p.is_deleted, 0) = 0 AND NVL(i.is_deleted, 0) = 0 "
                + "ORDER BY i.quantity ASC FETCH FIRST 5 ROWS ONLY";

        String sqlOrder = "SELECT 'Đơn hàng' AS loai, o.order_id, NVL(c.customer_name, 'Khách vãng lai'), o.status "
                + "FROM ORDERS o LEFT JOIN CUSTOMERS c ON o.customer_id = c.customer_id "
                + "WHERE o.status = N'Đang xử lý' AND NVL(o.is_deleted, 0) = 0 "
                + "ORDER BY o.order_date ASC FETCH FIRST 5 ROWS ONLY";

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlProd); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlOrder); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
            }
        }
        return tasks;
    }

    // 1. Biểu đồ Cột: Doanh thu 5 tháng gần nhất
    public Map<String, Double> getRevenueByMonth() throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT * FROM ( "
                + "  SELECT TO_CHAR(order_date, 'MM/YYYY') as month_year, MAX(order_date) as max_date, NVL(SUM(total_amount),0) as revenue "
                + "  FROM ORDERS WHERE NVL(is_deleted, 0) = 0 AND UPPER(NVL(status, '')) <> 'CANCELLED' "
                + "  GROUP BY TO_CHAR(order_date, 'MM/YYYY') "
                + "  ORDER BY max_date DESC "
                + ") WHERE ROWNUM <= 5";

        List<String> keys = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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

    // 2. Biểu đồ Đường: Đơn hàng 7 ngày gần nhất
    public Map<String, Integer> getOrdersByDay() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT * FROM ( "
                + "  SELECT TO_CHAR(order_date, 'DD/MM') as order_day, MAX(order_date) as max_date, COUNT(*) as order_count "
                + "  FROM ORDERS WHERE NVL(is_deleted, 0) = 0 "
                + "  GROUP BY TO_CHAR(order_date, 'DD/MM') "
                + "  ORDER BY max_date DESC "
                + ") WHERE ROWNUM <= 7";

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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

    // 3. Biểu đồ Tròn: Phân bổ sản phẩm theo Category
    public Map<String, Integer> getCategoryDistribution() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT NVL(category_id, 'Khác') as cat, COUNT(*) as cnt "
                + "FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0 GROUP BY category_id";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("cat"), rs.getInt("cnt"));
            }
        }
        return result;
    }

    // =========================================================
    // CÁC HÀM CHO STATISTICVIEW (BÁO CÁO CÓ BỘ LỌC NGÀY)
    // =========================================================
    /**
     * Báo cáo Doanh thu (Đã bỏ cột giảm giá, Tổng tiền hàng = Doanh thu thực)
     */
    public List<Object[]> getRevenueReport(java.util.Date fromDate, java.util.Date toDate) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT TO_CHAR(order_date, 'dd/MM/yyyy') as ngay, "
                + "       COUNT(order_id) as tong_don, "
                + "       SUM(total_amount) as doanh_thu_thuc "
                + "FROM ORDERS "
                + "WHERE NVL(is_deleted, 0) = 0 "
                + "AND UPPER(NVL(status, '')) <> 'CANCELLED' "
                + "AND order_date >= ? AND order_date < (? + 1) "
                + "GROUP BY TO_CHAR(order_date, 'dd/MM/yyyy') "
                + "ORDER BY TO_DATE(ngay, 'dd/MM/yyyy') DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            // Đưa Date Java util về Date SQL
            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String ngay = rs.getString("ngay");
                    int tongDon = rs.getInt("tong_don");
                    double doanhThu = rs.getDouble("doanh_thu_thuc");

                    String tienFormatted = currencyFormat.format(doanhThu);

                    // Trả ra mảng Object tương ứng với số cột bên View
                    rows.add(new Object[]{ngay, tongDon, tienFormatted, tienFormatted});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Báo cáo Sản phẩm (Tính số lượng và doanh thu từng mã)
     */
    public List<Object[]> getProductReport(java.util.Date fromDate, java.util.Date toDate) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT p.product_id, NVL(p.product_name, od.product_id) AS product_name, "
                + "       SUM(NVL(od.quantity_base, od.quantity)) AS sl_da_ban, "
                + "       SUM(NVL(od.quantity_base, od.quantity) * od.unit_price) AS doanh_thu_mang_lai, "
                + "       NVL(p.quantity, 0) AS ton_kho "
                + "FROM ORDER_DETAILS od "
                + "JOIN ORDERS o ON od.order_id = o.order_id "
                + "JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE NVL(od.is_deleted, 0) = 0 "
                + "  AND NVL(o.is_deleted, 0) = 0 "
                + "  AND UPPER(NVL(o.status, '')) <> 'CANCELLED' "
                + "  AND o.order_date >= ? AND o.order_date < (? + 1) "
                + "GROUP BY p.product_id, p.product_name, p.quantity "
                + "ORDER BY sl_da_ban DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("product_id");
                    String name = rs.getString("product_name");
                    int slBan = rs.getInt("sl_da_ban");
                    double doanhThu = rs.getDouble("doanh_thu_mang_lai");
                    int tonKho = rs.getInt("ton_kho");

                    rows.add(new Object[]{id, name, slBan, currencyFormat.format(doanhThu), tonKho});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Báo cáo Nhân viên (Lọc các đơn hàng tạo bởi nhân viên đó)
     */
    public List<Object[]> getEmployeeReport(java.util.Date fromDate, java.util.Date toDate) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT e.employee_id, e.employee_name, "
                + "       SUM(CASE WHEN UPPER(NVL(o.status, '')) = 'COMPLETED' THEN 1 ELSE 0 END) as don_thanh_cong, "
                + "       SUM(CASE WHEN UPPER(NVL(o.status, '')) = 'CANCELLED' THEN 1 ELSE 0 END) as don_huy, "
                + "       SUM(CASE WHEN UPPER(NVL(o.status, '')) = 'COMPLETED' THEN o.total_amount ELSE 0 END) as doanh_thu "
                + "FROM ORDERS o "
                + "JOIN EMPLOYEES e ON o.employee_id = e.employee_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 "
                + "  AND o.order_date >= ? AND o.order_date < (? + 1) "
                + "GROUP BY e.employee_id, e.employee_name "
                + "ORDER BY doanh_thu DESC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("employee_id");
                    String name = rs.getString("employee_name");
                    int donTC = rs.getInt("don_thanh_cong");
                    int donHuy = rs.getInt("don_huy");
                    double doanhThu = rs.getDouble("doanh_thu");

                    rows.add(new Object[]{id, name, donTC, donHuy, currencyFormat.format(doanhThu)});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }
}
