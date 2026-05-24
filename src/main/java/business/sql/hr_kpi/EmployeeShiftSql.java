package business.sql.hr_kpi;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.employee.EmployeeShift;

public class EmployeeShiftSql {

    private static final String TABLE_NAME = "EMPLOYEE_SHIFT_ASSIGNMENTS";

    public EmployeeShiftSql() {
        ensureStorage();
        migrateLegacyEmployeeShiftsIfExists();
    }

    public int insert(EmployeeShift item) {
        String sql = "INSERT INTO " + TABLE_NAME + " "
                + "(assignment_id, employee_id, shift_id, work_date, status, note, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0)";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getAssignmentId());
            ps.setString(2, item.getEmployeeId());
            ps.setString(3, item.getShiftId());
            ps.setDate(4, item.getWorkDate());
            ps.setString(5, item.getStatus() == null || item.getStatus().isBlank() ? "ASSIGNED" : item.getStatus());
            ps.setString(6, item.getNote());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int update(EmployeeShift item) {
        String sql = "UPDATE " + TABLE_NAME + " "
                + "SET employee_id = ?, shift_id = ?, work_date = ?, status = ?, note = ?, updated_at = SYSTIMESTAMP "
                + "WHERE assignment_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getEmployeeId());
            ps.setString(2, item.getShiftId());
            ps.setDate(3, item.getWorkDate());
            ps.setString(4, item.getStatus() == null || item.getStatus().isBlank() ? "ASSIGNED" : item.getStatus());
            ps.setString(5, item.getNote());
            ps.setString(6, item.getAssignmentId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int cancel(String assignmentId) {
        String sql = "UPDATE " + TABLE_NAME + " "
                + "SET status = 'CANCELED', updated_at = SYSTIMESTAMP "
                + "WHERE assignment_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, assignmentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int delete(String assignmentId) {
        String sql = "UPDATE " + TABLE_NAME + " "
                + "SET is_deleted = 1, updated_at = SYSTIMESTAMP "
                + "WHERE assignment_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, assignmentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int deleteMany(List<String> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE " + TABLE_NAME + " "
                + "SET is_deleted = 1, updated_at = SYSTIMESTAMP "
                + "WHERE assignment_id = ? AND NVL(is_deleted, 0) = 0";

        int total = 0;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            for (String id : assignmentIds) {
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                ps.setString(1, id.trim());
                total += ps.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return total;
    }

    public boolean existsDuplicate(String employeeId, String shiftId, Date workDate, String excludeAssignmentId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(TABLE_NAME).append(" ");
        sql.append("WHERE employee_id = ? AND shift_id = ? AND TRUNC(work_date) = TRUNC(?) ");
        sql.append("AND NVL(is_deleted, 0) = 0 AND NVL(status, 'ASSIGNED') <> 'CANCELED' ");

        if (excludeAssignmentId != null && !excludeAssignmentId.isBlank()) {
            sql.append("AND assignment_id <> ? ");
        }

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, employeeId);
            ps.setString(2, shiftId);
            ps.setDate(3, workDate);
            if (excludeAssignmentId != null && !excludeAssignmentId.isBlank()) {
                ps.setString(4, excludeAssignmentId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Giữ hàm cũ để không làm vỡ các nơi đang gọi theo 1 ngày.
    public List<EmployeeShift> selectAssignments(String keyword, Date workDate, String employeeType,
            String shiftId, String status) {
        return selectAssignments(keyword, workDate, workDate, employeeType, shiftId, status);
    }

    // Hàm mới: lọc theo khoảng ngày FromDate - ToDate để vẽ timeline.
    public List<EmployeeShift> selectAssignments(String keyword, Date fromDate, Date toDate, String employeeType,
            String shiftId, String status) {
        List<EmployeeShift> result = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT es.assignment_id, es.employee_id, e.employee_name, e.role_id, ");
        sql.append("TRUNC(es.work_date) AS work_date, es.shift_id, s.shift_name, ");
        sql.append("TO_CHAR(s.start_time, 'HH24:MI') AS start_time_text, ");
        sql.append("TO_CHAR(s.end_time, 'HH24:MI') AS end_time_text, ");
        sql.append("NVL(es.status, 'ASSIGNED') AS status, es.note ");
        sql.append("FROM ").append(TABLE_NAME).append(" es ");
        sql.append("JOIN EMPLOYEES e ON es.employee_id = e.employee_id ");
        sql.append("JOIN SHIFTS s ON es.shift_id = s.shift_id ");
        sql.append("WHERE NVL(es.is_deleted, 0) = 0 ");
        sql.append("AND NVL(e.is_deleted, 0) = 0 ");
        sql.append("AND NVL(s.is_deleted, 0) = 0 ");
        sql.append("AND e.role_id IN ('R_STAFF_SALE', 'R_STAFF_VIEW_PROD', 'R_STAFF_STOCK') ");

        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (LOWER(e.employee_name) LIKE ? OR LOWER(e.employee_id) LIKE ?) ");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (fromDate != null) {
            sql.append("AND TRUNC(es.work_date) >= TRUNC(?) ");
            params.add(fromDate);
        }

        if (toDate != null) {
            sql.append("AND TRUNC(es.work_date) <= TRUNC(?) ");
            params.add(toDate);
        }

        if (employeeType != null && !employeeType.isBlank() && !"ALL".equals(employeeType)) {
            if ("WAREHOUSE".equals(employeeType)) {
                sql.append("AND e.role_id IN ('R_STAFF_VIEW_PROD', 'R_STAFF_STOCK') ");
            } else if ("SALE".equals(employeeType)) {
                sql.append("AND e.role_id = 'R_STAFF_SALE' ");
            }
        }

        if (shiftId != null && !shiftId.isBlank()) {
            sql.append("AND es.shift_id = ? ");
            params.add(shiftId);
        }

        if (status != null && !status.isBlank()) {
            sql.append("AND NVL(es.status, 'ASSIGNED') = ? ");
            params.add(status);
        }

        sql.append("ORDER BY es.work_date ASC, e.employee_name ASC, s.start_time ASC");

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof Date dateValue) {
                    ps.setDate(i + 1, dateValue);
                } else {
                    ps.setString(i + 1, String.valueOf(value));
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    private EmployeeShift map(ResultSet rs) throws SQLException {
        EmployeeShift item = new EmployeeShift();
        item.setAssignmentId(rs.getString("assignment_id"));
        item.setEmployeeId(rs.getString("employee_id"));
        item.setEmployeeName(rs.getString("employee_name"));
        item.setEmployeeType(toEmployeeType(rs.getString("role_id")));
        item.setWorkDate(rs.getDate("work_date"));
        item.setShiftId(rs.getString("shift_id"));
        item.setShiftName(rs.getString("shift_name"));
        item.setStartTimeText(rs.getString("start_time_text"));
        item.setEndTimeText(rs.getString("end_time_text"));
        item.setStatus(rs.getString("status"));
        item.setNote(rs.getString("note"));
        return item;
    }

    private String toEmployeeType(String roleId) {
        if ("R_STAFF_VIEW_PROD".equalsIgnoreCase(roleId) || "R_STAFF_STOCK".equalsIgnoreCase(roleId)) {
            return "Nhân viên kho";
        }
        return "Nhân viên sale / Thu ngân";
    }

    private void ensureStorage() {
        String createSql = "CREATE TABLE " + TABLE_NAME + " ("
                + "assignment_id VARCHAR2(50) PRIMARY KEY, "
                + "employee_id VARCHAR2(50) NOT NULL, "
                + "shift_id VARCHAR2(50) NOT NULL, "
                + "work_date DATE NOT NULL, "
                + "status VARCHAR2(30) DEFAULT 'ASSIGNED' NOT NULL, "
                + "note NVARCHAR2(500), "
                + "is_deleted NUMBER(1) DEFAULT 0, "
                + "created_at TIMESTAMP DEFAULT SYSTIMESTAMP, "
                + "updated_at TIMESTAMP, "
                + "CONSTRAINT FK_ESA_EMPLOYEE FOREIGN KEY (employee_id) REFERENCES EMPLOYEES(employee_id), "
                + "CONSTRAINT FK_ESA_SHIFT FOREIGN KEY (shift_id) REFERENCES SHIFTS(shift_id)"
                + ")";

        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(createSql);
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {
                e.printStackTrace();
            }
        }
    }

    private void migrateLegacyEmployeeShiftsIfExists() {
        if (!tableExists("EMPLOYEE_SHIFTS")) {
            return;
        }

        String sql = "INSERT INTO " + TABLE_NAME + " "
                + "(assignment_id, employee_id, shift_id, work_date, status, note, is_deleted, created_at, updated_at) "
                + "SELECT old.assignment_id, old.employee_id, old.shift_id, old.work_date, "
                + "       NVL(old.status, 'ASSIGNED'), old.note, NVL(old.is_deleted, 0), "
                + "       NVL(old.created_at, SYSTIMESTAMP), old.updated_at "
                + "FROM EMPLOYEE_SHIFTS old "
                + "WHERE NOT EXISTS ( "
                + "    SELECT 1 FROM " + TABLE_NAME + " cur "
                + "    WHERE cur.assignment_id = old.assignment_id "
                + ")";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EmployeeShiftSql] migrateLegacyEmployeeShiftsIfExists error: " + e.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = UPPER(?)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
