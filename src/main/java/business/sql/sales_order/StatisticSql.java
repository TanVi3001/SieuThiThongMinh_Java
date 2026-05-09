package business.sql.sales_order;

import common.db.DatabaseConnection; // Lưu ý: dùng đúng package db của bạn
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;

public class StatisticSql {

    private static StatisticSql instance;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    public StatisticSql() {
    }

    public static StatisticSql getInstance() {
        if (instance == null) {
            instance = new StatisticSql();
        }
        return instance;
    }

    // =========================================================
    // 1. CÁC PHƯƠNG THỨC CŨ (GIỮ NGUYÊN ĐỂ KHÔNG LỖI HỆ THỐNG)
    // =========================================================
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
        String sql = "SELECT COUNT(*) FROM ORDERS WHERE NVL(is_deleted, 0) = 0 AND TRUNC(order_date) = TRUNC(SYSDATE)";
        return queryInt(sql);
    }

    public double getMonthlyRevenue() {
        String sql = "SELECT NVL(SUM(total_amount), 0) FROM ORDERS "
                + "WHERE NVL(is_deleted, 0) = 0 AND UPPER(NVL(status, '')) <> 'CANCELLED' "
                + "AND order_date >= TRUNC(SYSDATE, 'MM') AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)";
        return queryDouble(sql);
    }

    public List<Map<String, Object>> getBestSellingProducts(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT od.product_id, NVL(p.product_name, od.product_id) AS product_name, "
                + "SUM(NVL(od.quantity_base, od.quantity)) AS total_sold, SUM(NVL(od.quantity_base, od.quantity) * od.unit_price) AS total_revenue "
                + "FROM ORDER_DETAILS od JOIN ORDERS o ON od.order_id = o.order_id LEFT JOIN PRODUCTS p ON od.product_id = p.product_id "
                + "WHERE NVL(od.is_deleted, 0) = 0 AND NVL(o.is_deleted, 0) = 0 AND UPPER(NVL(o.status, '')) <> 'CANCELLED' "
                + "GROUP BY od.product_id, p.product_name ORDER BY total_sold DESC, total_revenue DESC) WHERE ROWNUM <= ?";
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
        String sql = "SELECT * FROM (SELECT o.order_id, NVL(c.customer_name, 'Khách vãng lai') AS customer_name, "
                + "o.total_amount, o.status, o.order_date FROM ORDERS o LEFT JOIN CUSTOMERS c ON o.customer_id = c.customer_id "
                + "WHERE NVL(o.is_deleted, 0) = 0 ORDER BY o.order_date DESC, o.order_id DESC) WHERE ROWNUM <= ?";
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

    // =========================================================
    // 2. CÁC PHƯƠNG THỨC BỔ SUNG CHO TONGQUANPANEL (DASHBOARD)
    // =========================================================
    // Thẻ Doanh thu hôm nay
    public double getTodayRevenue() {
        String sql = "SELECT NVL(SUM(total_amount), 0) FROM ORDERS "
                + "WHERE NVL(is_deleted, 0) = 0 AND (UPPER(status) = 'COMPLETED' OR UPPER(status) = N'HOÀN THÀNH') "
                + "AND TRUNC(order_date) = TRUNC(SYSDATE)";
        return queryDouble(sql);
    }

    // Lấy top sản phẩm tồn kho thấp (Dùng cho Progress Bars)
    public List<Map<String, Object>> getLowStockProducts(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT p.product_name, NVL(i.quantity, 0) as qty "
                + "FROM PRODUCTS p JOIN INVENTORY i ON p.product_id = i.product_id "
                + "WHERE p.is_deleted = 0 AND i.is_deleted = 0 ORDER BY i.quantity ASC) WHERE ROWNUM <= ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    // Biểu đồ Cột: Doanh thu 5 tháng
    public Map<String, Double> getRevenueByMonth() throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT * FROM (SELECT TO_CHAR(order_date, 'MM/YYYY') as month_year, MAX(order_date) as max_date, SUM(total_amount) as revenue "
                + "FROM ORDERS WHERE NVL(is_deleted, 0) = 0 AND (UPPER(status) = 'COMPLETED' OR UPPER(status) = N'HOÀN THÀNH') "
                + "GROUP BY TO_CHAR(order_date, 'MM/YYYY') ORDER BY max_date DESC) WHERE ROWNUM <= 5";

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

    // Biểu đồ Đường: Đơn hàng 7 ngày
    public Map<String, Integer> getOrdersByDay() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT * FROM (SELECT TO_CHAR(order_date, 'DD/MM') as order_day, MAX(order_date) as max_date, COUNT(*) as order_count "
                + "FROM ORDERS WHERE NVL(is_deleted, 0) = 0 GROUP BY TO_CHAR(order_date, 'DD/MM') ORDER BY max_date DESC) WHERE ROWNUM <= 7";

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

    // Biểu đồ Tròn: Phân bổ theo Category (Lấy tên Category thay vì ID)
    public Map<String, Integer> getCategoryDistribution() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT NVL(c.category_name, 'Khác') as cat, COUNT(*) as cnt "
                + "FROM PRODUCTS p LEFT JOIN CATEGORIES c ON p.category_id = c.category_id "
                + "WHERE NVL(p.is_deleted, 0) = 0 GROUP BY c.category_name";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("cat"), rs.getInt("cnt"));
            }
        }
        return result;
    }

    // =========================================================
    // 3. CÁC PHƯƠNG THỨC CHO STATISTICVIEW (BÁO CÁO CHI TIẾT)
    // =========================================================
    public List<Object[]> getRevenueReport(java.util.Date fromDate, java.util.Date toDate) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT TO_CHAR(TRUNC(order_date), 'dd/MM/yyyy') AS ngay, COUNT(order_id) AS tong_don, SUM(total_amount) as doanh_thu "
                + "FROM ORDERS WHERE NVL(is_deleted, 0) = 0 AND (UPPER(status) = 'COMPLETED' OR UPPER(status) = N'HOÀN THÀNH') "
                + "AND order_date >= ? AND order_date < (? + 1) GROUP BY TRUNC(order_date) ORDER BY TRUNC(order_date) DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            System.out.println("DEBUG - From Date: " + new java.sql.Date(fromDate.getTime()));
            System.out.println("DEBUG - To Date: " + new java.sql.Date(toDate.getTime()));
            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString("ngay"), rs.getInt("tong_don"), rs.getDouble("doanh_thu")});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<Object[]> getProductReport(java.util.Date fromDate, java.util.Date toDate) {
        List<Object[]> rows = new ArrayList<>();

        // SQL: Lấy tất cả sản phẩm và tính tổng số lượng bán + doanh thu từ chi tiết hóa đơn
        String sql = "SELECT p.product_id, p.product_name, "
                + "    NVL(sold.total_qty, 0) as qty_sold, "
                + "    NVL(sold.total_revenue, 0) as revenue, "
                + "    p.quantity as current_stock "
                + "FROM PRODUCTS p "
                + "LEFT JOIN ( "
                + "    SELECT d.product_id, "
                + "           SUM(d.quantity) as total_qty, "
                + "           SUM(d.quantity * d.price) as total_revenue "
                + "    FROM ORDER_DETAILS d "
                + "    INNER JOIN ORDERS o ON d.order_id = o.order_id "
                + "    WHERE NVL(o.is_deleted, 0) = 0 "
                + "      AND (UPPER(o.status) LIKE '%HOÀN THÀNH%' OR o.status = N'Hoàn thành') "
                + "      AND o.order_date >= ? AND o.order_date < (? + 1) "
                + "    GROUP BY d.product_id "
                + ") sold ON p.product_id = sold.product_id "
                + "WHERE NVL(p.is_deleted, 0) = 0 "
                + "ORDER BY qty_sold DESC, revenue DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString(1), // Mã SP
                        rs.getString(2), // Tên SP
                        rs.getInt(3), // Số lượng đã bán
                        rs.getDouble(4), // Doanh thu mang lại
                        rs.getInt(5) // Tồn kho hiện tại
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
        List<Object[]> rows = new ArrayList<>();

        // Câu SQL mới: Dùng LEFT JOIN bên trong Subquery để xử lý ID 
        // Tránh lỗi ORA-22818 (không cho dùng subquery trong GROUP BY)
        String sql = "SELECT e.employee_id, e.employee_name, "
                + "    NVL(agg.don_thanh_cong, 0), "
                + "    NVL(agg.don_huy, 0), "
                + "    NVL(agg.doanh_thu, 0) "
                + "FROM EMPLOYEES e "
                + "INNER JOIN ACCOUNTS a ON e.employee_id = a.user_id "
                + "LEFT JOIN ( "
                + "    SELECT "
                + "        NVL(a_ref.user_id, o.employee_id) as final_emp_id, "
                + "        COUNT(CASE WHEN (UPPER(o.status) LIKE '%HOÀN THÀNH%' OR o.status = N'Hoàn thành') THEN 1 END) as don_thanh_cong, "
                + "        COUNT(CASE WHEN (UPPER(o.status) LIKE '%HỦY%' OR o.status = N'Đã hủy') THEN 1 END) as don_huy, "
                + "        SUM(CASE WHEN (UPPER(o.status) LIKE '%HOÀN THÀNH%' OR o.status = N'Hoàn thành') THEN o.total_amount ELSE 0 END) as doanh_thu "
                + "    FROM ORDERS o "
                + "    LEFT JOIN ACCOUNTS a_ref ON o.employee_id = a_ref.account_id " // Bắc cầu qua đây
                + "    WHERE NVL(o.is_deleted, 0) = 0 "
                + "      AND o.order_date >= ? AND o.order_date < (? + 1) "
                + "    GROUP BY NVL(a_ref.user_id, o.employee_id) "
                + ") agg ON e.employee_id = agg.final_emp_id "
                + "WHERE NVL(e.is_deleted, 0) = 0 "
                + "  AND e.role_id = 'R_STAFF_SALE' "
                + "  AND (UPPER(a.status) LIKE '%HOẠT ĐỘNG%' OR a.status = N'Hoạt động') "
                + "ORDER BY doanh_thu DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getDouble(5)
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi báo cáo: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // =========================================================
    // HÀM TIỆN ÍCH TRUY VẤN NHANH
    // =========================================================
    private int queryInt(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private double queryDouble(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
