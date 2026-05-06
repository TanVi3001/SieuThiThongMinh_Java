package business.service;

import business.service.mail.MailSender;
import business.sql.rbac.ActivationTokenSql;
import common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Service cho Manager tạo nhân viên và gửi mã kích hoạt.
 */
public class EmployeeOnboardingService {

    private final ActivationTokenSql tokenSql = new ActivationTokenSql();
    private final MailSender mailSender;

    public EmployeeOnboardingService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    private Connection getConnection() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        if (con == null) {
            throw new SQLException("Không thể kết nối DB.");
        }
        return con;
    }

    /**
     * Manager tạo nhân viên -> insert EMPLOYEES -> tạo token -> commit -> gửi
     * email.
     *
     * @return activationCode
     */
    public String createEmployeeAndSendActivationCode(
            String employeeId,
            String employeeName,
            String phone,
            String email
    ) throws Exception {

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("EMPLOYEE_ID rỗng.");
        }
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("EMPLOYEE_NAME rỗng.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("EMAIL rỗng.");
        }

        Connection con = null;
        boolean oldAutoCommit = true;

        // code khó đoán
        String activationCode = UUID.randomUUID().toString(); // ví dụ: "a3f0...-...."

        try {
            con = getConnection();
            oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            // 1) Insert EMPLOYEES
            // LƯU Ý: nếu EMPLOYEES có cột NOT NULL khác (ROLE_ID/SHIFT_ID/HIRE_DATE...)
            // bạn phải bổ sung vào câu insert này theo schema thật.
            String sqlEmp
                    = "INSERT INTO EMPLOYEES (EMPLOYEE_ID, EMPLOYEE_NAME, PHONE, EMAIL, IS_DELETED) "
                    + "VALUES (?, ?, ?, ?, 0)";

            try (PreparedStatement ps = con.prepareStatement(sqlEmp)) {
                ps.setString(1, employeeId.trim());
                ps.setString(2, employeeName.trim());
                ps.setString(3, phone);
                ps.setString(4, email.trim());
                ps.executeUpdate();
            }

            // 2) Insert ACTIVATION_TOKENS (hết hạn 24h)
            tokenSql.createToken(con, employeeId.trim(), activationCode, 24);

            con.commit();

        } catch (Exception ex) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignore) {
                }
            }
            throw ex;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (SQLException ignore) {
                }
                DatabaseConnection.closeConnection(con);
            }
        }

        // 3) Gửi email SAU commit (để chắc DB đã có token)
        // Nếu gửi mail fail: vẫn giữ token, báo lỗi để manager biết.
        mailSender.sendActivationCode(email.trim(), employeeName.trim(), activationCode);

        return activationCode;
    }
}
