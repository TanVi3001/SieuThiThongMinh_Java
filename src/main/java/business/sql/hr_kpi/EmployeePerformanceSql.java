package business.hr_kpi;

import common.db.DatabaseConnection;
import common.db.DatabaseConnection;
import model.employee.EmployeePerformance;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeePerformanceSql {

    public List<EmployeePerformance> getAllEmployeeKPIs() {
        List<EmployeePerformance> list = new ArrayList<>();
        // Sử dụng CTE (WITH) để tính toán độc lập từng bảng, tránh lỗi nhân bản dữ liệu (Cartesian Product)
        String sql = """
            WITH OrderStats AS (
                SELECT employee_id, 
                       COUNT(order_id) as total_orders, 
                       NVL(SUM(total_amount), 0) as revenue,
                       SUM(CASE WHEN UPPER(status) = 'COMPLETED' THEN 1 ELSE 0 END) as completed_orders
                FROM ORDERS 
                WHERE NVL(is_deleted, 0) = 0 
                GROUP BY employee_id
            ),
            DeliveryStats AS (
                SELECT employee_id, 
                       COUNT(delivery_id) as total_deliveries,
                       SUM(CASE WHEN UPPER(status) = 'COMPLETED' THEN 1 ELSE 0 END) as success_deliveries
                FROM DELIVERY_MANAGEMENT 
                WHERE NVL(is_deleted, 0) = 0 
                GROUP BY employee_id
            ),
            AttendanceStats AS (
                SELECT employee_id, 
                       COUNT(work_date) as total_work_days,
                       ROUND(AVG(attendance_coefficient), 2) as avg_attendance_score
                FROM ATTENDANCE 
                WHERE NVL(is_deleted, 0) = 0 
                GROUP BY employee_id
            )
            SELECT 
                e.employee_id, e.employee_name,
                NVL(o.total_orders, 0) as total_orders,
                NVL(o.revenue, 0) as revenue,
                CASE WHEN NVL(o.total_orders, 0) = 0 THEN 0 ELSE ROUND(o.revenue / o.total_orders, 2) END as avg_order_value,
                CASE WHEN NVL(o.total_orders, 0) = 0 THEN 0 ELSE ROUND((o.completed_orders * 100.0) / o.total_orders, 2) END as completion_rate,
                NVL(d.total_deliveries, 0) as total_deliveries,
                CASE WHEN NVL(d.total_deliveries, 0) = 0 THEN 0 ELSE ROUND((d.success_deliveries * 100.0) / d.total_deliveries, 2) END as delivery_success_rate,
                NVL(a.total_work_days, 0) as total_work_days,
                NVL(a.avg_attendance_score, 0) as attendance_score
            FROM EMPLOYEES e
            LEFT JOIN OrderStats o ON e.employee_id = o.employee_id
            LEFT JOIN DeliveryStats d ON e.employee_id = d.employee_id
            LEFT JOIN AttendanceStats a ON e.employee_id = a.employee_id
            WHERE NVL(e.is_deleted, 0) = 0
            ORDER BY o.revenue DESC NULLS LAST
            """;

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                EmployeePerformance ep = new EmployeePerformance();
                ep.setEmployeeId(rs.getString("employee_id"));
                ep.setEmployeeName(rs.getString("employee_name"));
                ep.setTotalOrders(rs.getInt("total_orders"));
                ep.setRevenue(rs.getDouble("revenue"));
                ep.setAvgOrderValue(rs.getDouble("avg_order_value"));
                ep.setCompletionRate(rs.getDouble("completion_rate"));
                ep.setTotalDeliveries(rs.getInt("total_deliveries"));
                ep.setDeliverySuccessRate(rs.getDouble("delivery_success_rate"));
                ep.setTotalWorkDays(rs.getInt("total_work_days"));
                ep.setAttendanceScore(rs.getDouble("attendance_score"));

                // Tính toán điểm KPI tổng
                ep.calculatePerformanceScore();

                list.add(ep);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // Sửa hàm này để nhận tham số ngày

    public List<EmployeePerformance> getEmployeeKPIsByDate(Date fromDate, Date toDate) {
        List<EmployeePerformance> list = new ArrayList<>();
        // Chuyển java.util.Date sang java.sql.Date để query
        java.sql.Date sqlFrom = new java.sql.Date(fromDate.getTime());
        java.sql.Date sqlTo = new java.sql.Date(toDate.getTime());

        String sql = """
        WITH OrderStats AS (
            SELECT employee_id, 
                   COUNT(order_id) as total_orders, 
                   NVL(SUM(total_amount), 0) as revenue,
                   SUM(CASE WHEN UPPER(status) = 'COMPLETED' THEN 1 ELSE 0 END) as completed_orders,
                   SUM(CASE WHEN UPPER(status) = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_orders
            FROM ORDERS 
            WHERE NVL(is_deleted, 0) = 0 
              AND order_date BETWEEN ? AND ?
            GROUP BY employee_id
        ),
        AttendanceStats AS (
            SELECT employee_id, 
                   COUNT(work_date) as total_work_days,
                   ROUND(AVG(attendance_coefficient), 2) as avg_attendance_score
            FROM ATTENDANCE 
            WHERE NVL(is_deleted, 0) = 0 
              AND work_date BETWEEN ? AND ?
            GROUP BY employee_id
        )
        SELECT 
            e.employee_id, e.employee_name,
            NVL(o.completed_orders, 0) as completed_orders,
            NVL(o.cancelled_orders, 0) as cancelled_orders,
            NVL(o.revenue, 0) as revenue,
            NVL(o.total_orders, 0) as total_orders,
            NVL(a.avg_attendance_score, 0) as attendance_score
        FROM EMPLOYEES e
        LEFT JOIN OrderStats o ON e.employee_id = o.employee_id
        LEFT JOIN AttendanceStats a ON e.employee_id = a.employee_id
        WHERE NVL(e.is_deleted, 0) = 0
        ORDER BY o.revenue DESC NULLS LAST
        """;

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, sqlFrom);
            ps.setDate(2, sqlTo);
            ps.setDate(3, sqlFrom);
            ps.setDate(4, sqlTo);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                EmployeePerformance ep = new EmployeePerformance();
                ep.setEmployeeId(rs.getString("employee_id"));
                ep.setEmployeeName(rs.getString("employee_name"));
                ep.setTotalOrders(rs.getInt("completed_orders")); // Theo ảnh: Đơn hoàn thành
                ep.setTotalDeliveries(rs.getInt("cancelled_orders")); // Theo ảnh: Đơn bị hủy (tận dụng biến)
                ep.setRevenue(rs.getDouble("revenue"));
                // ... set các trường khác để tính KPI
                ep.calculatePerformanceScore();
                list.add(ep);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
