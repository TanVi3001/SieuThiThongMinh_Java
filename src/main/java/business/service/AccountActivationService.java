package business.service;

import business.sql.rbac.AccountActivationSql;
import business.sql.rbac.AccountAssignRoleSql;
import business.sql.rbac.ActivationTokenSql;
import business.sql.rbac.AuditLogSql;
import common.db.DatabaseConnection;
import common.sync.SyncVersionDao;
import common.realtime.RealtimeClient;
import model.account.ActivationEmployeeInfo;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;

public class AccountActivationService {

    private final AccountActivationSql accountActivationSql = AccountActivationSql.getInstance();
    private final ActivationTokenSql tokenSql = new ActivationTokenSql();

    public ActivationEmployeeInfo checkAndFetchActivation(String code) throws SQLException {
        if (code == null || code.isBlank()) {
            return null;
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            String empId = tokenSql.getEmployeeIdIfValid(con, code.trim());
            if (empId == null) {
                return null;
            }
            return accountActivationSql.getEmployeeInfo(con, empId);
        }
    }

    // Đổi thành void và throws Exception để UI hứng lỗi chi tiết
    public void activateAccount(String code, String username, String passwordPlain) throws Exception {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // TRANSACTION

            String empId = tokenSql.getEmployeeIdIfValid(con, code.trim());
            if (empId == null) {
                throw new Exception("Mã kích hoạt không hợp lệ hoặc đã hết hạn.");
            }

            ActivationEmployeeInfo info = accountActivationSql.getEmployeeInfo(con, empId);

            if (accountActivationSql.existsAccountByUsername(con, username.trim())) {
                throw new Exception("Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
            }

            String bcryptHash = BCrypt.hashpw(passwordPlain, BCrypt.gensalt(12));

            // Đảm bảo có USERS (Fix lỗi FK_ACCOUNTS_USERS)
            if (!accountActivationSql.existsUserById(con, empId)) {
                accountActivationSql.insertUser(con, empId, info.getFullName(), info.getEmail(), info.getPhone());
            }

            // Tạo ACCOUNTS và LẤY MÃ ACC (Quan trọng nhất)
            String actualRole = accountActivationSql.getEmployeeRole(con, empId);
            // Truyền actualRole vào lúc tạo ACCOUNTS
            String generatedAccId = accountActivationSql.insertAccount(con, empId, username.trim(), bcryptHash, actualRole);
            if (generatedAccId == null) {
                throw new Exception("Lỗi hệ thống khi tạo tài khoản.");
            }
            // Gán quyền dùng mã ACC (Fix lỗi FK_AAR_ACCOUNTS)
            AccountAssignRoleSql.getInstance().assignDefaultRole(con, generatedAccId, actualRole);

            // Cập nhật trạng thái "Đã cấp"
            accountActivationSql.updateEmployeeAccountStatus(con, empId);

            // Thu hồi mã kích hoạt
            tokenSql.markUsed(con, code.trim());

            // Ghi Log (Dùng mã ACC)
            AuditLogSql.getInstance().log(con, generatedAccId, "ACTIVATE_ACCOUNT", "Kích hoạt thành công: " + username);

            con.commit();

            // CẬP NHẬT GIAO DIỆN THEO THỜI GIAN THỰC
            try {
                SyncVersionDao.bumpVersion("ACCOUNT_SECURITY");
                RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                RealtimeClient.send("EMPLOYEES_CHANGED");
            } catch (Exception e) {
                System.err.println("Cảnh báo: Không thể gửi Realtime event.");
            }

        } catch (Exception e) {
            if (con != null) try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace(); // In lỗi gốc ra Console
            throw e; // Ném lỗi gốc ra cho UI
        } finally {
            if (con != null) try {
                DatabaseConnection.closeConnection(con);
            } catch (Exception ignore) {
            }
        }
    }
}
