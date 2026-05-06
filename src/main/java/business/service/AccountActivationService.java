package business.service;

import business.sql.rbac.*;
import common.db.DatabaseConnection;
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

    /**
     * Kích hoạt tài khoản: Tên tự do - Liên kết bằng ID.
     */
    public boolean activateAccount(String code, String username, String passwordPlain) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // BẮT ĐẦU GIAO DỊCH

            // 1. Lấy empId từ mã code
            String empId = tokenSql.getEmployeeIdIfValid(con, code.trim());
            if (empId == null) {
                throw new Exception("Mã không hợp lệ.");
            }

            // 2. Kiểm tra Username tự chọn đã có ai dùng chưa
            if (accountActivationSql.existsAccountByUsername(con, username.trim())) {
                throw new Exception("Tên đăng nhập đã tồn tại.");
            }

            // 3. Mã hóa BCrypt
            String bcryptHash = BCrypt.hashpw(passwordPlain, BCrypt.gensalt(12));

            // 4. Đảm bảo bản ghi USER tồn tại cho FK
            ActivationEmployeeInfo info = accountActivationSql.getEmployeeInfo(con, empId);
            if (!accountActivationSql.existsUserById(con, empId)) {
                accountActivationSql.insertUser(con, empId, info.getFullName(), info.getEmail(), info.getPhone());
            }

            // 5. TẠO TÀI KHOẢN & LẤY MÃ ACC...
            String generatedAccId = accountActivationSql.insertAccount(con, empId, username.trim(), bcryptHash, "R_STAFF_SALE");
            if (generatedAccId == null) {
                throw new Exception("Lỗi tạo tài khoản.");
            }

            // 6. GÁN QUYỀN: Dùng generatedAccId để khớp với ACCOUNT_ID trong DB[cite: 2]
            AccountAssignRoleSql.getInstance().assignDefaultRole(con, generatedAccId, "R_STAFF_SALE");

            // 7. Cập nhật trạng thái "Đã cấp"
            accountActivationSql.updateEmployeeAccountStatus(con, empId);

            // 8. Vô hiệu hóa mã Token
            tokenSql.markUsed(con, code.trim());

            // 9. GHI LOG: Dùng generatedAccId để định danh chính xác[cite: 2]
            AuditLogSql.getInstance().log(con, generatedAccId, "ACTIVATE_ACCOUNT",
                    "Nhân viên " + info.getFullName() + " đã tự kích hoạt tài khoản thành công.");

            con.commit(); // HOÀN TẤT
            return true;

        } catch (Exception e) {
            if (con != null) try {
                con.rollback();
            } catch (SQLException ex) {
            }
            System.err.println("BUG ACTIVATION: " + e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection(con);
        }
    }
}
