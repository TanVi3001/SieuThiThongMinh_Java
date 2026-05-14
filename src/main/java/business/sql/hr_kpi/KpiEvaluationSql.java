package business.sql.hr_kpi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.account.kpi.KpiEvaluation;
import model.account.kpi.KpiCriteria;

public class KpiEvaluationSql {
    public static KpiEvaluationSql getInstance() {
        return new KpiEvaluationSql();
    }

    public int insertEvaluation(Object t) { return 0; }

    // Hàm đặc thù lấy đánh giá theo nhân viên và kỳ đánh giá
    public ArrayList<Object> getEvaluationsByEmployee(String employeeId, String period) {
        return new ArrayList<>();
    }

    public ArrayList<KpiEvaluation> findByEmployeeId(String employeeId) {
        ArrayList<KpiEvaluation> list = new ArrayList<>();
        String sql = """
                SELECT
                    e.employee_id,
                    e.kpi_id,
                    NVL(c.criteria_name, e.kpi_id) AS criteria_name,
                    e.evaluation_period,
                    NVL(c.minimum_target, 0) AS minimum_target,
                    NVL(e.actual_value, 0) AS actual_value,
                    NVL(e.achieved_score, 0) AS achieved_score,
                    e.manager_note,
                    CASE
                        WHEN NVL(e.actual_value, 0) >= NVL(c.minimum_target, 0) THEN N'Đạt'
                        ELSE N'Chưa đạt'
                    END AS evaluation_status
                FROM KPI_EVALUATION e
                LEFT JOIN KPI_CRITERIA c ON e.kpi_id = c.kpi_id
                WHERE NVL(e.is_deleted, 0) = 0
                  AND e.employee_id = ?
                ORDER BY e.evaluation_period DESC, e.kpi_id ASC
                """;

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, employeeId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    KpiEvaluation evaluation = new KpiEvaluation();
                    evaluation.setEmployeeId(rs.getString("employee_id"));
                    evaluation.setKpiId(rs.getString("kpi_id"));
                    evaluation.setCriteriaName(rs.getString("criteria_name"));
                    evaluation.setEvaluationPeriod(rs.getString("evaluation_period"));
                    evaluation.setMinimumTarget(rs.getDouble("minimum_target"));
                    evaluation.setActualValue(rs.getDouble("actual_value"));
                    evaluation.setAchievedScore(rs.getDouble("achieved_score"));
                    evaluation.setManagerNote(rs.getString("manager_note"));
                    evaluation.setEvaluationStatus(rs.getString("evaluation_status"));
                    list.add(evaluation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public ArrayList<KpiCriteria> selectAll() {
        ArrayList<KpiCriteria> list = new ArrayList<>();
        String sql = "SELECT * FROM KPI_CRITERIA WHERE is_deleted = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                KpiCriteria k = new KpiCriteria();
                k.setKpiId(rs.getString("kpi_id"));
                k.setCriteriaName(rs.getString("criteria_name"));
                k.setCriteriaType(rs.getString("criteria_type"));
                k.setWeight(rs.getBigDecimal("weight"));
                k.setRecordedTime(rs.getTimestamp("recorded_time"));
                k.setMinimumTarget(rs.getBigDecimal("minimum_target"));
                k.setIsDeleted(rs.getInt("is_deleted"));
                list.add(k);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    
    /**
     * Lấy KPI evaluation cho nhân viên SALES ONLY (ẩn manager/admin)
     */
    public ArrayList<KpiEvaluation> findByEmployeeIdSalesOnly(String employeeId) {
        ArrayList<KpiEvaluation> list = new ArrayList<>();
        String sql = """
                SELECT
                    e.employee_id,
                    e.kpi_id,
                    NVL(c.criteria_name, e.kpi_id) AS criteria_name,
                    e.evaluation_period,
                    NVL(c.minimum_target, 0) AS minimum_target,
                    NVL(e.actual_value, 0) AS actual_value,
                    NVL(e.achieved_score, 0) AS achieved_score,
                    e.manager_note,
                    CASE
                        WHEN NVL(e.actual_value, 0) = 0 THEN N'Chưa bán'
                        WHEN NVL(e.actual_value, 0) >= NVL(c.minimum_target, 0) THEN N'Đạt'
                        ELSE N'Chưa đạt'
                    END AS evaluation_status
                FROM KPI_EVALUATION e
                LEFT JOIN KPI_CRITERIA c ON e.kpi_id = c.kpi_id
                INNER JOIN EMPLOYEES emp ON e.employee_id = emp.employee_id
                WHERE NVL(e.is_deleted, 0) = 0
                  AND e.employee_id = ?
                  AND UPPER(COALESCE(emp.role_id, '')) LIKE '%SALE%'
                ORDER BY e.evaluation_period DESC, e.kpi_id ASC
                """;

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, employeeId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    KpiEvaluation evaluation = new KpiEvaluation();
                    evaluation.setEmployeeId(rs.getString("employee_id"));
                    evaluation.setKpiId(rs.getString("kpi_id"));
                    evaluation.setCriteriaName(rs.getString("criteria_name"));
                    evaluation.setEvaluationPeriod(rs.getString("evaluation_period"));
                    evaluation.setMinimumTarget(rs.getDouble("minimum_target"));
                    evaluation.setActualValue(rs.getDouble("actual_value"));
                    evaluation.setAchievedScore(rs.getDouble("achieved_score"));
                    evaluation.setManagerNote(rs.getString("manager_note"));
                    evaluation.setEvaluationStatus(rs.getString("evaluation_status"));
                    list.add(evaluation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}