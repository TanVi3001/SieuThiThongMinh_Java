package business.sql.rbac;

import common.utils.PasswordUtils;
import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import model.account.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountSql implements SqlInterface<Account> {

    private static AccountSql instance;

    public AccountSql() {
    }

    public static AccountSql getInstance() {
        if (instance == null) {
            instance = new AccountSql();
        }
        return instance;
    }

    public Account selectByUsername(String username) {
        Account acc = null;
        String sql = "SELECT a.account_id, a.username, a.password, a.is_deleted, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.username = ? AND a.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    acc = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role_value"),
                            rs.getInt("is_deleted")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL Account.selectByUsername: " + e.getMessage());
            e.printStackTrace();
        }
        return acc;
    }

    @Override
    public List<Account> selectAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT a.account_id, a.username, a.password, a.is_deleted, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(new Account(
                        rs.getString("account_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role_value"),
                        rs.getInt("is_deleted")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public int insert(Account t) {
        return 0;
    }

    @Override
    public int update(Account t) {
        return 0;
    }

    @Override
    public int delete(String id) {
        String sql = "UPDATE ACCOUNTS SET is_deleted = 1 WHERE account_id = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Account selectById(String id) {
        String sql = "SELECT account_id, username, password, is_deleted FROM ACCOUNTS WHERE account_id = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getString("account_id"), rs.getString("username"), rs.getString("password"), null, rs.getInt("is_deleted"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Account> selectByCondition(String condition) {
        return new ArrayList<>();
    }

    public String findPassByUsernameAndEmail(String username, String email) {
        String passwordHash = null;
        String sql = "SELECT a.password FROM ACCOUNTS a JOIN USERS u ON a.user_id = u.user_id WHERE a.username = ? AND u.email = ? AND a.is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    passwordHash = rs.getString("password");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return passwordHash;
    }

    public boolean register(String fullName, String email, String phone, String username, String rawPassword) {
        String userId = "USR" + (System.currentTimeMillis() % 1000000);
        String accId = "ACC" + (System.currentTimeMillis() % 1000000);

        String sqlCheckUser = "SELECT 1 FROM ACCOUNTS WHERE username = ? AND is_deleted = 0";
        String sqlCheckEmail = "SELECT 1 FROM USERS WHERE email = ? AND is_deleted = 0";
        String sqlUser = "INSERT INTO USERS (user_id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";
        String sqlAccount = "INSERT INTO ACCOUNTS (account_id, user_id, username, password, status) VALUES (?, ?, ?, ?, 'Hoạt động')";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            boolean isFirstUser = false;
            String sqlCount = "SELECT COUNT(*) FROM ACCOUNTS";
            try (PreparedStatement pstCount = con.prepareStatement(sqlCount); ResultSet rsCount = pstCount.executeQuery()) {
                if (rsCount.next() && rsCount.getInt(1) == 0) {
                    isFirstUser = true;
                }
            }

            try (PreparedStatement pstCheckUser = con.prepareStatement(sqlCheckUser)) {
                pstCheckUser.setString(1, username);
                try (ResultSet rs = pstCheckUser.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement pstCheckEmail = con.prepareStatement(sqlCheckEmail)) {
                pstCheckEmail.setString(1, email);
                try (ResultSet rs = pstCheckEmail.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            String passwordHash = PasswordUtils.hash(rawPassword);

            try (PreparedStatement pstUser = con.prepareStatement(sqlUser); PreparedStatement pstAcc = con.prepareStatement(sqlAccount)) {

                pstUser.setString(1, userId);
                pstUser.setString(2, fullName);
                pstUser.setString(3, email);
                pstUser.setString(4, phone);
                pstUser.executeUpdate();

                pstAcc.setString(1, accId);
                pstAcc.setString(2, userId);
                pstAcc.setString(3, username);
                pstAcc.setString(4, passwordHash);
                pstAcc.executeUpdate();

                String roleId = isFirstUser ? "R_ADMIN_ALL" : "R_STAFF_SALE";

                String sqlAssignRole = "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id) VALUES (?, ?)";
                try (PreparedStatement pstRole = con.prepareStatement(sqlAssignRole)) {
                    pstRole.setString(1, accId);
                    pstRole.setString(2, roleId);
                    pstRole.executeUpdate();
                }
            }

            con.commit();
            return true;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean updatePasswordByAccountId(String accountId, String passwordHash) {
        String sql = "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, passwordHash);
            pst.setString(2, accountId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int migratePlainPasswordsToBCrypt() {
        int migrated = 0;
        String sqlSelect = "SELECT account_id, password FROM ACCOUNTS WHERE is_deleted = 0";
        String sqlUpdate = "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pstSelect = con.prepareStatement(sqlSelect); ResultSet rs = pstSelect.executeQuery(); PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
            while (rs.next()) {
                String accountId = rs.getString("account_id");
                String pwd = rs.getString("password");
                if (pwd != null && !PasswordUtils.isBCryptHash(pwd)) {
                    String hash = PasswordUtils.hash(pwd);
                    pstUpdate.setString(1, hash);
                    pstUpdate.setString(2, accountId);
                    pstUpdate.addBatch();
                    migrated++;
                }
            }
            pstUpdate.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return migrated;
    }

    public String findUsernameByEmail(String email) {
        String sql = "SELECT a.username FROM ACCOUNTS a JOIN USERS u ON a.user_id = u.user_id WHERE u.email = ? AND a.is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveOTP(String email, String otp) {
        String sql = "MERGE INTO OTP_STORAGE t USING (SELECT ? as email, ? as otp FROM dual) s ON (t.email = s.email) "
                + "WHEN MATCHED THEN UPDATE SET t.otp_code = s.otp, t.expiry_time = sysdate + 5/1440 "
                + "WHEN NOT MATCHED THEN INSERT (email, otp_code, expiry_time) VALUES (s.email, s.otp, sysdate + 5/1440)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            pst.setString(2, otp);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateOTP(String email, String otp) {
        String sql = "SELECT 1 FROM OTP_STORAGE WHERE email = ? AND otp_code = ? AND expiry_time > sysdate";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            pst.setString(2, otp);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Giữ nguyên hàm cũ: input là RAW password -> tự hash.
     */
    public boolean updatePasswordByEmail(String email, String rawPassword) {
        String passwordHash = PasswordUtils.hash(rawPassword);
        String sql = "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = (SELECT user_id FROM USERS WHERE email = ?)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, passwordHash);
            pst.setString(2, email);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HÀM MỚI (CHỐT DOUBLE-HASH): input là PASSWORD HASH sẵn (BCrypt) -> update
     * thẳng.
     */
    public boolean updatePasswordHashByEmail(String email, String passwordHash) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        String sql = "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = (SELECT user_id FROM USERS WHERE email = ?)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, passwordHash);
            pst.setString(2, email);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkDuplicateUsername(String username) {
        String sql = "SELECT 1 FROM ACCOUNTS WHERE username = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkDuplicateEmail(String email) {
        String sql = "SELECT 1 FROM USERS WHERE email = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkDuplicatePhone(String phone) {
        String sql = "SELECT 1 FROM USERS WHERE phone_number = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, phone);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getAccountWithUserDetails() {
        List<String[]> list = new ArrayList<>();

        String sql = "SELECT a.account_id, a.username, u.full_name, u.email, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value, "
                + "       a.is_deleted "
                + "FROM ACCOUNTS a "
                + "JOIN USERS u ON a.user_id = u.user_id "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("account_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("role_value"),
                    String.valueOf(rs.getInt("is_deleted"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateAccountRole(String accountId, String newRoleId) {
        String sqlCheck = "SELECT 1 FROM ACCOUNT_ASSIGN_ROLE WHERE account_id = ?";
        String sqlUpdate = "UPDATE ACCOUNT_ASSIGN_ROLE SET role_id = ? WHERE account_id = ?";
        String sqlInsert = "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id) VALUES (?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement pstCheck = con.prepareStatement(sqlCheck)) {
                pstCheck.setString(1, accountId);
                try (ResultSet rs = pstCheck.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                try (PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
                    pstUpdate.setString(1, newRoleId);
                    pstUpdate.setString(2, accountId);
                    return pstUpdate.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement pstInsert = con.prepareStatement(sqlInsert)) {
                    pstInsert.setString(1, accountId);
                    pstInsert.setString(2, newRoleId);
                    return pstInsert.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createEmployeeAccount(String fullName, String email, String phone, String username, String roleId) {
        String userId = "USR" + (System.currentTimeMillis() % 1000000);
        String accId = "ACC" + (System.currentTimeMillis() % 1000000);
        String rawPassword = "1234";

        String sqlCheckUser = "SELECT 1 FROM ACCOUNTS WHERE username = ? AND is_deleted = 0";
        String sqlCheckEmail = "SELECT 1 FROM USERS WHERE email = ? AND is_deleted = 0";
        String sqlUser = "INSERT INTO USERS (user_id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";
        String sqlAccount = "INSERT INTO ACCOUNTS (account_id, user_id, username, password, status) VALUES (?, ?, ?, ?, 'Hoạt động')";
        String sqlAssignRole = "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id) VALUES (?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement pstCheckUser = con.prepareStatement(sqlCheckUser)) {
                pstCheckUser.setString(1, username);
                try (ResultSet rs = pstCheckUser.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
            try (PreparedStatement pstCheckEmail = con.prepareStatement(sqlCheckEmail)) {
                pstCheckEmail.setString(1, email);
                try (ResultSet rs = pstCheckEmail.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }

            String passwordHash = PasswordUtils.hash(rawPassword);

            try (PreparedStatement pstUser = con.prepareStatement(sqlUser); PreparedStatement pstAcc = con.prepareStatement(sqlAccount); PreparedStatement pstRole = con.prepareStatement(sqlAssignRole)) {

                pstUser.setString(1, userId);
                pstUser.setString(2, fullName);
                pstUser.setString(3, email);
                pstUser.setString(4, phone);
                pstUser.executeUpdate();

                pstAcc.setString(1, accId);
                pstAcc.setString(2, userId);
                pstAcc.setString(3, username);
                pstAcc.setString(4, passwordHash);
                pstAcc.executeUpdate();

                pstRole.setString(1, accId);
                pstRole.setString(2, roleId);
                pstRole.executeUpdate();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public java.util.Map<String, String> getEmployeeForActivation(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        java.util.Map<String, String> data = new java.util.HashMap<>();

        String sqlToken
                = "SELECT EMPLOYEE_ID "
                + "FROM ACTIVATION_TOKENS "
                + "WHERE CODE = ? "
                + "AND USED_AT IS NULL "
                + "AND EXPIRES_AT >= SYSDATE";

        String sqlCheckExistAcc
                = "SELECT 1 FROM ACCOUNTS WHERE user_id = ? AND NVL(is_deleted,0)=0";

        String sqlEmp
                = "SELECT employee_name, email, phone, role_id "
                + "FROM EMPLOYEES "
                + "WHERE employee_id = ? AND NVL(is_deleted,0)=0";

        try (Connection con = DatabaseConnection.getConnection()) {
            if (con == null) {
                return null;
            }

            // 1) Verify token -> lấy employeeId
            String empId;
            try (PreparedStatement pst = con.prepareStatement(sqlToken)) {
                pst.setString(1, code.trim());
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    empId = rs.getString("EMPLOYEE_ID");
                }
            }

            // 2) Nếu đã có account thì không cho kích hoạt lại
            try (PreparedStatement pstCheck = con.prepareStatement(sqlCheckExistAcc)) {
                pstCheck.setString(1, empId);
                try (ResultSet rsCheck = pstCheck.executeQuery()) {
                    if (rsCheck.next()) {
                        return null;
                    }
                }
            }

            // 3) Lấy thông tin nhân viên
            try (PreparedStatement pst = con.prepareStatement(sqlEmp)) {
                pst.setString(1, empId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    data.put("emp_id", empId); // quan trọng: trả lại empId để stage2 dùng
                    data.put("name", rs.getString("employee_name"));
                    data.put("email", rs.getString("email"));
                    data.put("phone", rs.getString("phone"));
                    data.put("role_id", rs.getString("role_id"));
                    return data;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean activateAccount(String code, String username, String rawPassword) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (username == null || username.isBlank()) {
            return false;
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        String accId = "ACC" + (System.currentTimeMillis() % 1000000);

        // đảm bảo BCrypt để qua trigger
        String passwordHash = PasswordUtils.isBCryptHash(rawPassword)
                ? rawPassword
                : PasswordUtils.hash(rawPassword);

        String sqlToken
                = "SELECT EMPLOYEE_ID "
                + "FROM ACTIVATION_TOKENS "
                + "WHERE CODE = ? AND USED_AT IS NULL AND EXPIRES_AT >= SYSDATE";

        String sqlMarkUsed
                = "UPDATE ACTIVATION_TOKENS SET USED_AT = SYSDATE "
                + "WHERE CODE = ? AND USED_AT IS NULL";

        String sqlCheckUser
                = "SELECT 1 FROM ACCOUNTS WHERE username = ? AND is_deleted = 0";

        String sqlGetEmp
                = "SELECT employee_name, email, phone, role_id "
                + "FROM EMPLOYEES WHERE employee_id = ? AND is_deleted = 0";

        String sqlUser
                = "INSERT INTO USERS (user_id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";

        String sqlAccount
                = "INSERT INTO ACCOUNTS (account_id, user_id, username, password, status) VALUES (?, ?, ?, ?, 'Hoạt động')";

        String sqlRole
                = "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id) VALUES (?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            if (con == null) {
                return false;
            }
            con.setAutoCommit(false);

            // 1) Verify token -> empId
            String empId;
            try (PreparedStatement pst = con.prepareStatement(sqlToken)) {
                pst.setString(1, code.trim());
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    empId = rs.getString("EMPLOYEE_ID");
                }
            }

            // 2) Check username duplicate
            try (PreparedStatement pst = con.prepareStatement(sqlCheckUser)) {
                pst.setString(1, username.trim());
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            // 3) Get employee info
            String name, email, phone, roleId;
            try (PreparedStatement pst = con.prepareStatement(sqlGetEmp)) {
                pst.setString(1, empId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    name = rs.getString("employee_name");
                    email = rs.getString("email");
                    phone = rs.getString("phone");
                    roleId = rs.getString("role_id");
                }
            }

            // 4) Insert USERS (user_id = empId)
            try (PreparedStatement pst = con.prepareStatement(sqlUser)) {
                pst.setString(1, empId);
                pst.setString(2, name);
                pst.setString(3, email);
                pst.setString(4, phone);
                pst.executeUpdate();
            }

            // 5) Insert ACCOUNTS
            try (PreparedStatement pst = con.prepareStatement(sqlAccount)) {
                pst.setString(1, accId);
                pst.setString(2, empId);
                pst.setString(3, username.trim());
                pst.setString(4, passwordHash);
                pst.executeUpdate();
            }

            // 6) Assign role
            try (PreparedStatement pst = con.prepareStatement(sqlRole)) {
                pst.setString(1, accId);
                pst.setString(2, roleId);
                pst.executeUpdate();
            }

            // 7) Mark token used
            try (PreparedStatement pst = con.prepareStatement(sqlMarkUsed)) {
                pst.setString(1, code.trim());
                pst.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void insertAccount(Connection conn, String username, String hashedPassword, String empId) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
