package business.sql.rbac;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL layer cho bảng ACTIVATION_TOKENS
 */
public class ActivationTokenSql {

    private static final String TBL = "ACTIVATION_TOKENS";

    /**
     * Tạo token mới cho employee, hết hạn sau N giờ.
     *
     * @return code đã insert
     */
    public String createToken(Connection con, String employeeId, String code, int expiresHours) throws SQLException {
        String sql
                = "INSERT INTO " + TBL + " (EMPLOYEE_ID, CODE, EXPIRES_AT, USED_AT, CREATED_AT) "
                + "VALUES (?, ?, SYSDATE + (?/24), NULL, SYSDATE)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setString(2, code);
            ps.setInt(3, expiresHours);
            ps.executeUpdate();
            return code;
        }
    }

    /**
     * Kiểm tra code hợp lệ: - tồn tại - chưa USED - chưa hết hạn
     *
     * @return employeeId nếu hợp lệ, null nếu không hợp lệ
     */
    public String getEmployeeIdIfValid(Connection con, String code) throws SQLException {
        String sql
                = "SELECT EMPLOYEE_ID "
                + "FROM " + TBL + " "
                + "WHERE CODE = ? "
                + "AND USED_AT IS NULL "
                + "AND EXPIRES_AT >= SYSDATE";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("EMPLOYEE_ID");
            }
        }
    }

    public void markUsed(Connection con, String code) throws SQLException {
        String sql
                = "UPDATE " + TBL + " SET USED_AT = SYSDATE WHERE CODE = ? AND USED_AT IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    public boolean existsCode(Connection con, String code) throws SQLException {
        String sql = "SELECT 1 FROM " + TBL + " WHERE CODE = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
