package business.sql.hr_kpi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.account.kpi.KpiCriteria;
import model.account.kpi.KpiEvaluation;

public class KpiEvaluationSql {
    public static KpiEvaluationSql getInstance() {
        return new KpiEvaluationSql();
    }

    public int insertEvaluation(Object t) { return 0; }

    // Hàm đặc thù lấy đánh giá theo nhân viên và kỳ đánh giá
    public ArrayList<Object> getEvaluationsByEmployee(String employeeId, String period) {
        return new ArrayList<>();
    }
    
    /**
     * Lấy danh sách lịch sử đánh giá KPI của một nhân viên theo các tháng
     * @param employeeId Mã nhân viên
     * @return Danh sách KpiEvaluation sắp xếp theo tháng giảm dần
     */
    public ArrayList<KpiEvaluation> findByEmployeeId(String employeeId) {
        ArrayList<KpiEvaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM KPI_EVALUATION WHERE employee_id = ? AND is_deleted = 0 ORDER BY evaluation_period DESC";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, employeeId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    KpiEvaluation k = new KpiEvaluation();
                    k.setEmployeeId(rs.getString("employee_id"));
                    k.setKpiId(rs.getString("kpi_id"));
                    k.setEvaluationPeriod(rs.getString("evaluation_period"));
                    k.setActualValue(rs.getDouble("actual_value"));
                    k.setAchievedScore(rs.getDouble("achieved_score"));
                    k.setManagerNote(rs.getString("manager_note"));
                    k.setIsDeleted(rs.getInt("is_deleted"));
                    list.add(k);
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
}