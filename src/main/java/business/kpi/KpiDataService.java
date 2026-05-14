package business.kpi;

import model.employee.EmployeePerformance;
import model.account.kpi.KpiEvaluation;
import model.account.kpi.KpiCriteria;
import common.db.DatabaseConnection;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service để quản lý KPI nhân viên trong database
 * Hỗ trợ import dữ liệu từ CSV, tính điểm KPI, chỉ bao gồm nhân viên bán hàng (SALES role)
 * Nhân viên chưa bán được đơn nào sẽ không được tính đạt KPI
 */
public class KpiDataService {

    private static final String SALES_ROLE = "SALES";

    /**
     * Kiểm tra xem nhân viên có tồn tại và có phải SALES không
     */
    public static boolean isSalesEmployee(String employeeId) {
        String sql = """
            SELECT COUNT(*) FROM EMPLOYEES 
            WHERE employee_id = ? 
              AND NVL(is_deleted, 0) = 0
              AND UPPER(COALESCE(role_id, '')) LIKE '%SALE%'
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy employee_id từ các loại định danh khác nhau (mã, tên, email, điện thoại)
     * Chỉ trả về nhân viên SALES
     */
    public static String resolveEmployeeId(String identifier, String employeeName) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String lookupSql = """
            SELECT employee_id FROM EMPLOYEES 
            WHERE NVL(is_deleted, 0) = 0
              AND UPPER(COALESCE(role_id, '')) LIKE '%SALE%'
              AND (
                   UPPER(employee_id) = UPPER(?)
                   OR UPPER(employee_name) LIKE UPPER(?)
                   OR UPPER(email) = UPPER(?)
                   OR UPPER(phone) = UPPER(?)
              )
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(lookupSql)) {
            ps.setString(1, identifier);
            ps.setString(2, "%" + identifier + "%");
            ps.setString(3, identifier);
            ps.setString(4, identifier);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("employee_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Tải các tiêu chí KPI mặc định từ database
     */
    public static List<KpiCriteria> loadActiveCriteria() {
        List<KpiCriteria> criteria = new ArrayList<>();
        String sql = "SELECT * FROM KPI_CRITERIA WHERE NVL(is_deleted, 0) = 0 ORDER BY kpi_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                KpiCriteria c = new KpiCriteria();
                c.setKpiId(rs.getString("kpi_id"));
                c.setCriteriaName(rs.getString("criteria_name"));
                c.setCriteriaType(rs.getString("criteria_type"));
                c.setWeight(rs.getBigDecimal("weight"));
                c.setRecordedTime(rs.getTimestamp("recorded_time"));
                c.setMinimumTarget(rs.getBigDecimal("minimum_target"));
                c.setIsDeleted(rs.getInt("is_deleted"));
                criteria.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return criteria;
    }

    /**
     * Seed dữ liệu KPI_CRITERIA mặc định nếu bảng trống
     */
    public static void seedDefaultCriteria(Connection conn) {
        try {
            // Kiểm tra xem đã có dữ liệu không
            String checkSql = "SELECT COUNT(*) FROM KPI_CRITERIA WHERE NVL(is_deleted, 0) = 0";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Đã có dữ liệu, không cần seed
                }
            }

            // Insert 5 tiêu chí KPI mặc định
            String insertSql = """
                INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
                VALUES (?, ?, ?, ?, SYSTIMESTAMP, ?, 0)
                """;

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                Object[][] criteriaData = {
                    {"KPI_ORDERS", "Số đơn hoàn thành", "SALES", 0.25, 50},
                    {"KPI_REVENUE", "Doanh thu", "SALES", 0.25, 10000000},
                    {"KPI_COMPLETION", "Tỷ lệ hoàn thành", "SERVICE", 0.20, 90},
                    {"KPI_DELIVERY", "Tỷ lệ giao hàng", "SERVICE", 0.15, 90},
                    {"KPI_ATTENDANCE", "Điểm chuyên cần", "ATTENDANCE", 0.15, 8}
                };

                for (Object[] data : criteriaData) {
                    ps.setString(1, (String) data[0]);
                    ps.setString(2, (String) data[1]);
                    ps.setString(3, (String) data[2]);
                    ps.setBigDecimal(4, BigDecimal.valueOf(((Number) data[3]).doubleValue()));
                    ps.setBigDecimal(5, BigDecimal.valueOf(((Number) data[4]).doubleValue()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi seed KPI_CRITERIA: " + e.getMessage());
        }
    }

    /**
     * Tìm tiêu chí KPI theo loại hoặc tên
     */
    public static KpiCriteria findCriteriaForMetric(String metricType, List<KpiCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }

        String searchTerm = metricType.toUpperCase();
        for (KpiCriteria c : criteriaList) {
            if (c.getCriteriaType() != null && c.getCriteriaType().toUpperCase().contains(searchTerm)) {
                return c;
            }
            if (c.getCriteriaName() != null && c.getCriteriaName().toUpperCase().contains(searchTerm)) {
                return c;
            }
            if (c.getKpiId() != null && c.getKpiId().toUpperCase().contains(searchTerm)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Import dữ liệu KPI từ danh sách EmployeePerformance
     * CHÚ Ý: Nếu nhân viên chưa bán được đơn nào (total_orders = 0), 
     * thì achieved_score sẽ được đặt = 0 (chưa đạt KPI)
     */
    public static void importKpiData(List<EmployeePerformance> performanceList) {
        if (performanceList == null || performanceList.isEmpty()) {
            System.out.println("Danh sách dữ liệu trống");
            return;
        }

        List<KpiCriteria> criteriaList = loadActiveCriteria();
        if (criteriaList.isEmpty()) {
            System.err.println("Không có tiêu chí KPI nào. Hãy seed dữ liệu trước.");
            return;
        }

        String insertSql = """
            INSERT INTO KPI_EVALUATION (employee_id, kpi_id, evaluation_period, actual_value, achieved_score, is_deleted)
            VALUES (?, ?, ?, ?, ?, 0)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            conn.setAutoCommit(false);
            int successCount = 0;

            for (EmployeePerformance perf : performanceList) {
                // Kiểm tra nhân viên có phải SALES không
                if (!isSalesEmployee(perf.getEmployeeId())) {
                    System.err.println("⚠️ Bỏ qua: " + perf.getEmployeeId() + " - không phải nhân viên bán hàng");
                    continue;
                }

                // NẾU CHƯA BÁN ĐƠN NÀO, KHÔNG ĐƯỢC TÍNH ĐẠT KPI
                if (perf.getTotalOrders() <= 0) {
                    System.out.println("⚠️ " + perf.getEmployeeId() + " chưa bán được đơn nào - không được tính đạt KPI");
                }

                String period = getCurrentPeriod();

                // Insert KPI_ORDERS
                ps.setString(1, perf.getEmployeeId());
                ps.setString(2, "KPI_ORDERS");
                ps.setString(3, period);
                ps.setDouble(4, perf.getTotalOrders());
                ps.setDouble(5, perf.getTotalOrders() > 0 ? 100 : 0);
                ps.addBatch();
                successCount++;

                // Insert KPI_REVENUE
                ps.setString(1, perf.getEmployeeId());
                ps.setString(2, "KPI_REVENUE");
                ps.setString(3, period);
                ps.setDouble(4, perf.getRevenue());
                ps.setDouble(5, perf.getTotalOrders() > 0 ? 100 : 0);
                ps.addBatch();
                successCount++;

                // Insert KPI_COMPLETION
                ps.setString(1, perf.getEmployeeId());
                ps.setString(2, "KPI_COMPLETION");
                ps.setString(3, period);
                ps.setDouble(4, perf.getCompletionRate());
                ps.setDouble(5, perf.getTotalOrders() > 0 && perf.getCompletionRate() >= 90 ? 100 : 0);
                ps.addBatch();
                successCount++;

                // Insert KPI_DELIVERY
                ps.setString(1, perf.getEmployeeId());
                ps.setString(2, "KPI_DELIVERY");
                ps.setString(3, period);
                ps.setDouble(4, perf.getDeliverySuccessRate());
                ps.setDouble(5, perf.getTotalOrders() > 0 && perf.getDeliverySuccessRate() >= 90 ? 100 : 0);
                ps.addBatch();
                successCount++;

                // Insert KPI_ATTENDANCE
                ps.setString(1, perf.getEmployeeId());
                ps.setString(2, "KPI_ATTENDANCE");
                ps.setString(3, period);
                ps.setDouble(4, perf.getAttendanceScore());
                ps.setDouble(5, perf.getTotalOrders() > 0 && perf.getAttendanceScore() >= 8 ? 100 : 0);
                ps.addBatch();
                successCount++;

                // Batch mỗi 50 dòng (10 nhân viên × 5 KPI)
                if (successCount % 50 == 0) {
                    ps.executeBatch();
                }
            }

            // Execute các dòng còn lại
            ps.executeBatch();
            conn.commit();
            System.out.println("✅ Import thành công " + successCount + " KPI records");

        } catch (SQLException e) {
            System.err.println("❌ Lỗi import KPI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy kỳ đánh giá hiện tại (MM-YYYY)
     */
    public static String getCurrentPeriod() {
        LocalDate now = LocalDate.now();
        return String.format("%02d-%04d", now.getMonthValue(), now.getYear());
    }
}
