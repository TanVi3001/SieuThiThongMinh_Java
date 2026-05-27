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

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String buildAccountId() {
        return "ACC" + System.currentTimeMillis();
    }

    private static String normalizePhoneSql(String columnName) {
        return "REGEXP_REPLACE(NVL(" + columnName + ", ''), '[^0-9]', '')";
    }

    // =========================================================
    // SESSION MANAGEMENT
    // =========================================================
    public boolean increaseActiveSession(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = CASE "
                + "        WHEN LAST_HEARTBEAT_AT >= SYSTIMESTAMP - INTERVAL '30' SECOND "
                + "        THEN GREATEST(NVL(ACTIVE_SESSIONS, 0), 1) "
                + "        ELSE 1 "
                + "    END, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    LAST_LOGIN_AT = CURRENT_TIMESTAMP, "
                + "    LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] increaseActiveSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean decreaseActiveSession(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = GREATEST(NVL(ACTIVE_SESSIONS, 0) - 1, 0), "
                + "    ONLINE_STATUS = CASE "
                + "        WHEN GREATEST(NVL(ACTIVE_SESSIONS, 0) - 1, 0) > 0 THEN 'ONLINE' "
                + "        ELSE 'OFFLINE' "
                + "    END, "
                + "    LAST_LOGOUT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] decreaseActiveSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean heartbeat(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] heartbeat error: " + e.getMessage());
            return false;
        }
    }

    public boolean normalizeOnlineStatus(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = CASE "
                + "        WHEN LAST_HEARTBEAT_AT >= SYSTIMESTAMP - INTERVAL '30' SECOND THEN 1 "
                + "        ELSE 0 "
                + "    END, "
                + "    ONLINE_STATUS = CASE "
                + "        WHEN LAST_HEARTBEAT_AT >= SYSTIMESTAMP - INTERVAL '30' SECOND THEN 'ONLINE' "
                + "        ELSE 'OFFLINE' "
                + "    END, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] normalizeOnlineStatus error: " + e.getMessage());
            return false;
        }
    }

    public boolean isAccountStoreActive(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        String sql = """
        SELECT
            CASE
                WHEN e.store_id IS NULL THEN 1
                WHEN NVL(s.is_deleted, 0) = 0 THEN 1
                ELSE 0
            END AS store_active
        FROM ACCOUNTS a
        LEFT JOIN EMPLOYEES e
            ON e.employee_id = a.user_id
           AND NVL(e.is_deleted, 0) = 0
        LEFT JOIN STORES s
            ON s.store_id = e.store_id
        WHERE a.account_id = ?
          AND NVL(a.is_deleted, 0) = 0
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("store_active") == 1;
                }
            }

        } catch (Exception e) {
            System.err.println("[AccountSql] isAccountStoreActive error: " + e.getMessage());
        }

        return false;
    }

    public Account selectByUsername(String username) {
        Account acc = null;

        String sql
                = "SELECT a.account_id, a.user_id, a.username, a.password, a.status, a.is_deleted, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id "
                + "      AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id "
                + "      AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id "
                + "      AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.username = ? "
                + "  AND NVL(a.is_deleted, 0) = 0 "
                + "  AND UPPER(TRIM(NVL(a.status, N'Hoạt động'))) NOT IN "
                + "      (N'BỊ KHÓA', 'BI KHOA', 'LOCKED', 'DISABLED', 'INACTIVE', N'TẠM KHÓA', 'TAM KHOA')";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

                    acc.setUserId(rs.getString("user_id"));
                    acc.setStatus(rs.getString("status"));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Cannot query account data during login.", e);
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
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        String sql = ""
                + "SELECT a.account_id, a.user_id, a.username, a.password, a.status, a.is_deleted, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id "
                + "      AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id "
                + "      AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id "
                + "      AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.account_id = ? "
                + "  AND NVL(a.is_deleted, 0) = 0";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id.trim());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Account acc = new Account(
                            rs.getString("account_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role_value"),
                            rs.getInt("is_deleted")
                    );

                    acc.setUserId(rs.getString("user_id"));
                    acc.setStatus(rs.getString("status"));

                    return acc;
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
        String sqlUser = ""
                + "MERGE INTO USERS u "
                + "USING ( "
                + "    SELECT ? AS user_id, ? AS full_name, ? AS email, ? AS phone_number FROM dual "
                + ") src "
                + "ON (u.user_id = src.user_id) "
                + "WHEN MATCHED THEN UPDATE SET "
                + "    u.full_name = src.full_name, "
                + "    u.email = src.email, "
                + "    u.phone_number = src.phone_number, "
                + "    u.is_deleted = 0 "
                + "WHEN NOT MATCHED THEN INSERT (user_id, full_name, email, phone_number, is_deleted) "
                + "VALUES (src.user_id, src.full_name, src.email, src.phone_number, 0)";
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
                + "       a.is_deleted, "
                + "       NVL(a.online_status, 'OFFLINE') AS online_status "
                + "FROM ACCOUNTS a "
                + "JOIN USERS u ON a.user_id = u.user_id "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar "
                + "       ON a.account_id = aar.account_id AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg "
                + "       ON a.account_id = aarg.account_id AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg "
                + "       ON aarg.role_group_id = rg.role_group_id AND NVL(rg.is_deleted, 0) = 0 ";
        // BỎ HẲN DÒNG WHERE a.is_deleted = 0 Ở ĐÂY ĐỂ LẤY CẢ TÀI KHOẢN BỊ KHÓA

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("account_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("role_value"),
                    String.valueOf(rs.getInt("is_deleted")),
                    rs.getString("online_status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateAccountRole(String accountId, String newRoleId) {
        if (accountId == null || accountId.trim().isEmpty()
                || newRoleId == null || newRoleId.trim().isEmpty()) {
            return false;
        }

        String cleanAccountId = accountId.trim();
        String cleanRoleId = newRoleId.trim().toUpperCase();

        String sqlGetUserId = """
            SELECT user_id
            FROM ACCOUNTS
            WHERE account_id = ?
            FETCH FIRST 1 ROWS ONLY
        """;

        String sqlDisableOldRoleGroups = """
            UPDATE ACCOUNT_ASSIGN_ROLE_GROUP
            SET is_deleted = 1
            WHERE account_id = ?
        """;

        String sqlDisableOldDirectRoles = """
            UPDATE ACCOUNT_ASSIGN_ROLE
            SET is_deleted = 1
            WHERE account_id = ?
        """;

        /*
         * Không dùng MERGE ON(account_id) vì dữ liệu cũ có thể đã có nhiều dòng role
         * cho cùng một account_id. Cách an toàn hơn:
         * 1) khóa mềm toàn bộ role cũ;
         * 2) update một dòng role hiện có thành role mới;
         * 3) nếu chưa có dòng nào thì insert mới.
         */
        String sqlReuseOneDirectRoleRow = """
            UPDATE ACCOUNT_ASSIGN_ROLE
            SET role_id = ?,
                is_deleted = 0
            WHERE ROWID = (
                SELECT ROWID
                FROM ACCOUNT_ASSIGN_ROLE
                WHERE account_id = ?
                FETCH FIRST 1 ROWS ONLY
            )
        """;

        String sqlInsertDirectRole = """
            INSERT INTO ACCOUNT_ASSIGN_ROLE (
                account_id,
                role_id,
                is_deleted
            )
            VALUES (?, ?, 0)
        """;

        String sqlUpdateEmployeeRole = """
            UPDATE EMPLOYEES
            SET role_id = ?
            WHERE employee_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlTouchAccount = """
            UPDATE ACCOUNTS
            SET updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
        """;

        Connection con = null;
        boolean oldAutoCommit = true;

        try {
            con = DatabaseConnection.getConnection();
            if (con == null) {
                return false;
            }

            oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            String userId = null;

            try (PreparedStatement ps = con.prepareStatement(sqlGetUserId)) {
                ps.setString(1, cleanAccountId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getString("user_id");
                    }
                }
            }

            if (userId == null || userId.trim().isEmpty()) {
                con.rollback();
                System.err.println("[AccountSql] updateAccountRole failed: ACCOUNT_NOT_FOUND " + cleanAccountId);
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDisableOldRoleGroups)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDisableOldDirectRoles)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
            }

            int roleRows;
            try (PreparedStatement ps = con.prepareStatement(sqlReuseOneDirectRoleRow)) {
                ps.setString(1, cleanRoleId);
                ps.setString(2, cleanAccountId);
                roleRows = ps.executeUpdate();
            }

            if (roleRows <= 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlInsertDirectRole)) {
                    ps.setString(1, cleanAccountId);
                    ps.setString(2, cleanRoleId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateEmployeeRole)) {
                ps.setString(1, cleanRoleId);
                ps.setString(2, userId.trim());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlTouchAccount)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
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

            System.err.println("[AccountSql] updateAccountRole error: " + e.getMessage());
            e.printStackTrace();
            return false;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (Exception ignored) {
                }

                try {
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean updateAccountRoleByEmployeeId(String employeeId, String newRoleId) {
        if (employeeId == null || employeeId.trim().isEmpty()
                || newRoleId == null || newRoleId.trim().isEmpty()) {
            return false;
        }

        String cleanEmployeeId = employeeId.trim();
        String cleanRoleId = newRoleId.trim().toUpperCase();

        /*
         * Không lọc NVL(is_deleted,0)=0 ở ACCOUNTS tại đây.
         * Tài khoản bị khóa vẫn phải được đồng bộ role, nếu không EMPLOYEES.role_id
         * và ACCOUNT_ASSIGN_ROLE.role_id sẽ lệch nhau.
         */
        String sql = """
            SELECT account_id
            FROM ACCOUNTS
            WHERE user_id = ?
            ORDER BY NVL(is_deleted, 0), updated_at DESC NULLS LAST, account_id DESC
            FETCH FIRST 1 ROWS ONLY
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cleanEmployeeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String accountId = rs.getString("account_id");
                    return updateAccountRole(accountId, cleanRoleId);
                }
            }

            System.err.println("[AccountSql] updateAccountRoleByEmployeeId failed: ACCOUNT_NOT_FOUND_BY_EMPLOYEE " + cleanEmployeeId);

        } catch (Exception e) {
            System.err.println("[AccountSql] updateAccountRoleByEmployeeId error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean createEmployeeAccount(String fullName, String email, String phone, String username, String roleId) {
        String cleanUsername = clean(username);
        String cleanEmail = clean(email);
        String cleanPhone = clean(phone);
        String cleanRoleId = clean(roleId);

        if (cleanUsername == null || cleanRoleId == null) {
            return false;
        }

        String accId = buildAccountId();
        String rawPassword = "1234";

        String sqlFindEmployee = ""
                + "SELECT employee_id, employee_name, email, phone, role_id, store_id "
                + "FROM EMPLOYEES "
                + "WHERE NVL(is_deleted, 0) = 0 "
                + "  AND ( "
                + "        (? IS NOT NULL AND LOWER(TRIM(email)) = LOWER(TRIM(?))) "
                + "     OR (? IS NOT NULL AND "
                + "        REGEXP_REPLACE(NVL(phone, ''), '[^0-9]', '') = "
                + "        REGEXP_REPLACE(NVL(?, ''), '[^0-9]', '') "
                + "     ) "
                + "  ) "
                + "ORDER BY CASE WHEN LOWER(TRIM(email)) = LOWER(TRIM(?)) THEN 1 ELSE 2 END "
                + "FETCH FIRST 1 ROWS ONLY";

        String sqlCheckUsername = ""
                + "SELECT 1 FROM ACCOUNTS "
                + "WHERE username = ? "
                + "  AND NVL(is_deleted, 0) = 0";

        String sqlCheckEmployeeAccount = ""
                + "SELECT 1 FROM ACCOUNTS "
                + "WHERE user_id = ? "
                + "  AND NVL(is_deleted, 0) = 0";

        String sqlUpsertUser = ""
                + "MERGE INTO USERS u "
                + "USING (SELECT ? AS user_id, ? AS full_name, ? AS email, ? AS phone_number FROM dual) src "
                + "ON (u.user_id = src.user_id) "
                + "WHEN MATCHED THEN UPDATE SET "
                + "    u.full_name = src.full_name, "
                + "    u.email = src.email, "
                + "    u.phone_number = src.phone_number, "
                + "    u.is_deleted = 0 "
                + "WHEN NOT MATCHED THEN INSERT (user_id, full_name, email, phone_number, is_deleted) "
                + "VALUES (src.user_id, src.full_name, src.email, src.phone_number, 0)";

        String sqlAccount = ""
                + "INSERT INTO ACCOUNTS (account_id, user_id, username, password, status, is_deleted) "
                + "VALUES (?, ?, ?, ?, 'Hoạt động', 0)";

        String sqlAssignRole = ""
                + "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id, is_deleted) "
                + "VALUES (?, ?, 0)";

        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            String employeeId;
            String employeeName;
            String employeeEmail;
            String employeePhone;
            String employeeRoleId;
            String storeId;

            try (PreparedStatement ps = con.prepareStatement(sqlFindEmployee)) {
                ps.setString(1, cleanEmail);
                ps.setString(2, cleanEmail);
                ps.setString(3, cleanPhone);
                ps.setString(4, cleanPhone);
                ps.setString(5, cleanEmail);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        System.err.println("[AccountSql] EMPLOYEE_NOT_FOUND_BY_EMAIL_OR_PHONE");
                        return false;
                    }

                    employeeId = rs.getString("employee_id");
                    employeeName = rs.getString("employee_name");
                    employeeEmail = rs.getString("email");
                    employeePhone = rs.getString("phone");
                    employeeRoleId = rs.getString("role_id");
                    storeId = rs.getString("store_id");
                }
            }

            if (clean(employeeId) == null) {
                con.rollback();
                return false;
            }

            if (clean(storeId) == null) {
                con.rollback();
                System.err.println("[AccountSql] EMPLOYEE_WITHOUT_STORE: " + employeeId);
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sqlCheckUsername)) {
                ps.setString(1, cleanUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        System.err.println("[AccountSql] DUPLICATE_USERNAME");
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlCheckEmployeeAccount)) {
                ps.setString(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        System.err.println("[AccountSql] EMPLOYEE_ALREADY_HAS_ACCOUNT: " + employeeId);
                        return false;
                    }
                }
            }

            String finalName = clean(employeeName) != null ? clean(employeeName) : clean(fullName);
            String finalEmail = clean(employeeEmail) != null ? clean(employeeEmail) : cleanEmail;
            String finalPhone = clean(employeePhone) != null ? clean(employeePhone) : cleanPhone;
            String finalRoleId = clean(employeeRoleId) != null ? clean(employeeRoleId) : cleanRoleId;

            String passwordHash = PasswordUtils.hash(rawPassword);

            try (PreparedStatement ps = con.prepareStatement(sqlUpsertUser)) {
                ps.setString(1, employeeId);
                ps.setString(2, finalName);
                ps.setString(3, finalEmail);
                ps.setString(4, finalPhone);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlAccount)) {
                ps.setString(1, accId);
                ps.setString(2, employeeId);
                ps.setString(3, cleanUsername);
                ps.setString(4, passwordHash);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlAssignRole)) {
                ps.setString(1, accId);
                ps.setString(2, finalRoleId);
                ps.executeUpdate();
            }

            con.commit();

            System.out.println("[AccountSql] createEmployeeAccount success: "
                    + "accountId=" + accId
                    + ", employeeId=" + employeeId
                    + ", username=" + cleanUsername
                    + ", roleId=" + finalRoleId
                    + ", storeId=" + storeId);

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
                = "SELECT e.employee_name, e.email, e.phone, e.role_id, e.store_id, s.store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN STORES s "
                + "       ON s.store_id = e.store_id "
                + "      AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE e.employee_id = ? "
                + "  AND NVL(e.is_deleted, 0) = 0";
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

                    data.put("emp_id", empId);
                    data.put("name", rs.getString("employee_name"));
                    data.put("email", rs.getString("email"));
                    data.put("phone", rs.getString("phone"));
                    data.put("role_id", rs.getString("role_id"));
                    data.put("store_id", rs.getString("store_id"));
                    data.put("store_name", rs.getString("store_name"));
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

        String accId = buildAccountId();

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

        String sqlUser = ""
                + "MERGE INTO USERS u "
                + "USING (SELECT ? AS user_id, ? AS full_name, ? AS email, ? AS phone_number FROM dual) src "
                + "ON (u.user_id = src.user_id) "
                + "WHEN MATCHED THEN UPDATE SET "
                + "    u.full_name = src.full_name, "
                + "    u.email = src.email, "
                + "    u.phone_number = src.phone_number, "
                + "    u.is_deleted = 0 "
                + "WHEN NOT MATCHED THEN INSERT (user_id, full_name, email, phone_number, is_deleted) "
                + "VALUES (src.user_id, src.full_name, src.email, src.phone_number, 0)";

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

    /**
     * LẤY THÔNG TIN CHI TIẾT ĐỂ SECURITY GUARD KIỂM TRA (Đã fix lỗi JOIN và
     * Connection)
     */
    public String[] getAccountDetails(String accountId) {
        String sql = "SELECT a.account_id, a.username, u.full_name, u.email, "
                + "       COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value, "
                + "       a.is_deleted "
                + "FROM ACCOUNTS a "
                + "JOIN USERS u ON a.user_id = u.user_id "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE aar ON a.account_id = aar.account_id AND NVL(aar.is_deleted, 0) = 0 "
                + "LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg ON a.account_id = aarg.account_id AND NVL(aarg.is_deleted, 0) = 0 "
                + "LEFT JOIN ROLE_GROUPS rg ON aarg.role_group_id = rg.role_group_id AND NVL(rg.is_deleted, 0) = 0 "
                + "WHERE a.account_id = ?";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        rs.getString("account_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role_value"),
                        String.valueOf(rs.getInt("is_deleted"))
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL AccountSql.getAccountDetails: " + e.getMessage());
        }
        return null;
    }
    // =========================================================
// ONLINE / OFFLINE STATUS
// =========================================================

    public boolean updateOnlineStatus(String accountId, String onlineStatus) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

        String status = "ONLINE".equalsIgnoreCase(onlineStatus) ? "ONLINE" : "OFFLINE";

        String sql
                = "UPDATE ACCOUNTS "
                + "SET online_status = ?, "
                + "    last_login_at = CASE WHEN ? = 'ONLINE' THEN CURRENT_TIMESTAMP ELSE last_login_at END, "
                + "    last_logout_at = CASE WHEN ? = 'OFFLINE' THEN CURRENT_TIMESTAMP ELSE last_logout_at END, "
                + "    updated_at = CURRENT_TIMESTAMP "
                + "WHERE account_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, status);
            pst.setString(2, status);
            pst.setString(3, status);
            pst.setString(4, accountId);

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi AccountSql.updateOnlineStatus: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean setOnline(String accountId) {
        return updateOnlineStatus(accountId, "ONLINE");
    }

    public boolean setOffline(String accountId) {
        return updateOnlineStatus(accountId, "OFFLINE");
    }

    public String getOnlineStatus(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "OFFLINE";
        }

        String sql = "SELECT NVL(online_status, 'OFFLINE') AS online_status "
                + "FROM ACCOUNTS "
                + "WHERE account_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, accountId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("online_status");
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi AccountSql.getOnlineStatus: " + e.getMessage());
            e.printStackTrace();
        }

        return "OFFLINE";
    }

    public boolean setAllAccountsOffline() {
        String sql = "UPDATE ACCOUNTS "
                + "SET online_status = 'OFFLINE', "
                + "    last_logout_at = CURRENT_TIMESTAMP, "
                + "    updated_at = CURRENT_TIMESTAMP "
                + "WHERE NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            return pst.executeUpdate() >= 0;

        } catch (SQLException e) {
            System.err.println("Lỗi AccountSql.setAllAccountsOffline: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean activateSingleSession(String accountId, String sessionId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = 1, "
                + "    CURRENT_SESSION_ID = ?, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    LAST_LOGIN_AT = CURRENT_TIMESTAMP, "
                + "    LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sessionId);
            ps.setString(2, accountId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] activateSingleSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean logoutBySession(String accountId, String sessionId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = 0, "
                + "    CURRENT_SESSION_ID = NULL, "
                + "    ONLINE_STATUS = 'OFFLINE', "
                + "    LAST_LOGOUT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND CURRENT_SESSION_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            ps.setString(2, sessionId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] logoutBySession error: " + e.getMessage());
            return false;
        }
    }

    public boolean heartbeatBySession(String accountId, String sessionId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    ACTIVE_SESSIONS = 1, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND CURRENT_SESSION_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            ps.setString(2, sessionId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] heartbeatBySession error: " + e.getMessage());
            return false;
        }
    }

    public boolean isCurrentSessionValid(String accountId, String sessionId) {
        String sql
                = "SELECT COUNT(*) "
                + "FROM ACCOUNTS "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND CURRENT_SESSION_ID = ? "
                + "  AND NVL(ACTIVE_SESSIONS, 0) = 1 "
                + "  AND ONLINE_STATUS = 'ONLINE' "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            ps.setString(2, sessionId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            System.err.println("[AccountSql] isCurrentSessionValid error: " + e.getMessage());
            return false;
        }
    }

    public int resetDeadSessions() {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = 0, "
                + "    CURRENT_SESSION_ID = NULL, "
                + "    ONLINE_STATUS = 'OFFLINE', "
                + "    LAST_LOGOUT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE NVL(IS_DELETED, 0) = 0 "
                + "  AND NVL(ACTIVE_SESSIONS, 0) = 1 "
                + "  AND LAST_HEARTBEAT_AT < SYSTIMESTAMP - INTERVAL '2' MINUTE";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            return ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("[AccountSql] resetDeadSessions error: " + e.getMessage());
            return 0;
        }
    }

    public boolean activateMultiSession(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = NVL(ACTIVE_SESSIONS, 0) + 1, "
                + "    CURRENT_SESSION_ID = NULL, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    LAST_LOGIN_AT = CURRENT_TIMESTAMP, "
                + "    LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] activateMultiSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean logoutMultiSession(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET ACTIVE_SESSIONS = GREATEST(NVL(ACTIVE_SESSIONS, 0) - 1, 0), "
                + "    ONLINE_STATUS = CASE "
                + "        WHEN GREATEST(NVL(ACTIVE_SESSIONS, 0) - 1, 0) > 0 THEN 'ONLINE' "
                + "        ELSE 'OFFLINE' "
                + "    END, "
                + "    LAST_LOGOUT_AT = CURRENT_TIMESTAMP, "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] logoutMultiSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean heartbeatMultiSession(String accountId) {
        String sql
                = "UPDATE ACCOUNTS "
                + "SET LAST_HEARTBEAT_AT = CURRENT_TIMESTAMP, "
                + "    ONLINE_STATUS = 'ONLINE', "
                + "    UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE ACCOUNT_ID = ? "
                + "  AND NVL(IS_DELETED, 0) = 0 "
                + "  AND NVL(ACTIVE_SESSIONS, 0) > 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AccountSql] heartbeatMultiSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean createLoginSession(String accountId, String sessionId) {
        String sql = """
        MERGE INTO ACCOUNT_SESSIONS s
        USING (
            SELECT ? AS session_id,
                   ? AS account_id
            FROM dual
        ) src
        ON (s.session_id = src.session_id)
        WHEN MATCHED THEN
            UPDATE SET
                s.account_id = src.account_id,
                s.last_heartbeat_at = CURRENT_TIMESTAMP,
                s.status = 'ACTIVE',
                s.is_deleted = 0,
                s.logout_at = NULL
        WHEN NOT MATCHED THEN
            INSERT (
                session_id,
                account_id,
                login_at,
                last_heartbeat_at,
                status,
                device_info,
                ip_address,
                is_deleted
            )
            VALUES (
                src.session_id,
                src.account_id,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                'ACTIVE',
                ?,
                ?,
                0
            )
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sessionId);
            ps.setString(2, accountId);
            ps.setString(3, System.getProperty("os.name") + " | Java " + System.getProperty("java.version"));
            ps.setString(4, "local");

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] createLoginSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean heartbeatSession(String accountId, String sessionId) {
        String sql = """
        UPDATE ACCOUNT_SESSIONS
        SET last_heartbeat_at = CURRENT_TIMESTAMP,
            status = 'ACTIVE'
        WHERE account_id = ?
          AND session_id = ?
          AND NVL(is_deleted, 0) = 0
          AND status = 'ACTIVE'
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            ps.setString(2, sessionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] heartbeatSession error: " + e.getMessage());
            return false;
        }
    }

    public boolean closeLoginSession(String accountId, String sessionId) {
        String sql = """
        UPDATE ACCOUNT_SESSIONS
        SET status = 'LOGOUT',
            logout_at = CURRENT_TIMESTAMP,
            is_deleted = 1
        WHERE account_id = ?
          AND session_id = ?
          AND NVL(is_deleted, 0) = 0
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);
            ps.setString(2, sessionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AccountSql] closeLoginSession error: " + e.getMessage());
            return false;
        }
    }

    public int countActiveSessions(String accountId) {
        String sql = """
        SELECT COUNT(*)
        FROM ACCOUNT_SESSIONS
        WHERE account_id = ?
          AND status = 'ACTIVE'
          AND NVL(is_deleted, 0) = 0
          AND last_heartbeat_at >= SYSTIMESTAMP - INTERVAL '30' SECOND
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[AccountSql] countActiveSessions error: " + e.getMessage());
        }

        return 0;
    }

    public void cleanupDeadSessions() {
        String sql = """
        UPDATE ACCOUNT_SESSIONS
        SET status = 'EXPIRED',
            is_deleted = 1,
            logout_at = CURRENT_TIMESTAMP
        WHERE status = 'ACTIVE'
          AND NVL(is_deleted, 0) = 0
          AND last_heartbeat_at < SYSTIMESTAMP - INTERVAL '30' SECOND
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[AccountSql] cleanupDeadSessions error: " + e.getMessage());
        }
    }

    /**
     * Ép đăng xuất một tài khoản khỏi mọi session. Dùng khi Admin/Manager khóa
     * tài khoản.
     *
     * Lưu ý: - Không filter NVL(IS_DELETED,0)=0 vì lúc khóa xong account đã
     * is_deleted=1. - Mục tiêu là dọn trạng thái online ảo và đóng toàn bộ
     * ACCOUNT_SESSIONS.
     */
    public boolean forceLogoutAccount(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        String cleanAccountId = accountId.trim();

        String sqlAccount = """
        UPDATE ACCOUNTS
        SET ACTIVE_SESSIONS = 0,
            CURRENT_SESSION_ID = NULL,
            ONLINE_STATUS = 'OFFLINE',
            LAST_LOGOUT_AT = CURRENT_TIMESTAMP,
            UPDATED_AT = CURRENT_TIMESTAMP
        WHERE ACCOUNT_ID = ?
    """;

        String sqlSessions = """
        UPDATE ACCOUNT_SESSIONS
        SET STATUS = 'FORCED_LOGOUT',
            IS_DELETED = 1,
            LOGOUT_AT = CURRENT_TIMESTAMP
        WHERE ACCOUNT_ID = ?
          AND NVL(IS_DELETED, 0) = 0
          AND STATUS = 'ACTIVE'
    """;

        Connection con = null;
        boolean oldAutoCommit = true;

        try {
            con = DatabaseConnection.getConnection();
            oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlSessions)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
            }

            int updated;
            try (PreparedStatement ps = con.prepareStatement(sqlAccount)) {
                ps.setString(1, cleanAccountId);
                updated = ps.executeUpdate();
            }

            con.commit();
            return updated > 0;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }

            System.err.println("[AccountSql] forceLogoutAccount error: " + e.getMessage());
            return false;

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (Exception ignored) {
                }

                try {
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Dọn online ảo: - Account bị khóa -> OFFLINE - Account không còn session
     * active trong 30s -> OFFLINE - Account còn session active trong 30s ->
     * ONLINE
     */
    public int syncOnlineStatusFromSessions() {
        cleanupDeadSessions();

        String sql = """
        UPDATE ACCOUNTS a
        SET a.ONLINE_STATUS =
                CASE
                    WHEN NVL(a.IS_DELETED, 0) = 1 THEN 'OFFLINE'
                    WHEN EXISTS (
                        SELECT 1
                        FROM ACCOUNT_SESSIONS s
                        WHERE s.ACCOUNT_ID = a.ACCOUNT_ID
                          AND s.STATUS = 'ACTIVE'
                          AND NVL(s.IS_DELETED, 0) = 0
                          AND s.LAST_HEARTBEAT_AT >= SYSTIMESTAMP - INTERVAL '30' SECOND
                    ) THEN 'ONLINE'
                    ELSE 'OFFLINE'
                END,
            a.ACTIVE_SESSIONS =
                CASE
                    WHEN NVL(a.IS_DELETED, 0) = 1 THEN 0
                    WHEN EXISTS (
                        SELECT 1
                        FROM ACCOUNT_SESSIONS s
                        WHERE s.ACCOUNT_ID = a.ACCOUNT_ID
                          AND s.STATUS = 'ACTIVE'
                          AND NVL(s.IS_DELETED, 0) = 0
                          AND s.LAST_HEARTBEAT_AT >= SYSTIMESTAMP - INTERVAL '30' SECOND
                    ) THEN 1
                    ELSE 0
                END,
            a.CURRENT_SESSION_ID =
                CASE
                    WHEN NVL(a.IS_DELETED, 0) = 1 THEN NULL
                    ELSE a.CURRENT_SESSION_ID
                END,
            a.UPDATED_AT = CURRENT_TIMESTAMP
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("[AccountSql] syncOnlineStatusFromSessions error: " + e.getMessage());
            return 0;
        }
    }

    public boolean canLoginByCurrentShift(String accountId, String roleId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        // Admin cho đăng nhập luôn.
        if ("R_ADMIN_ALL".equalsIgnoreCase(roleId)) {
            return true;
        }

        String sql = """
        SELECT COUNT(*) AS valid_shift_count
        FROM ACCOUNTS a
        JOIN EMPLOYEES e
            ON e.employee_id = a.user_id
        JOIN EMPLOYEE_SHIFT_ASSIGNMENTS esa
            ON esa.employee_id = e.employee_id
        JOIN SHIFTS s
            ON s.shift_id = esa.shift_id
        WHERE a.account_id = ?
          AND NVL(a.is_deleted, 0) = 0
          AND NVL(e.is_deleted, 0) = 0
          AND NVL(esa.is_deleted, 0) = 0
          AND NVL(s.is_deleted, 0) = 0
          AND esa.status = 'ASSIGNED'
          AND (
                s.shift_id = 'SHIFT_FULLTIME'
                OR
                (
                    TO_CHAR(s.start_time, 'HH24:MI:SS') <= TO_CHAR(s.end_time, 'HH24:MI:SS')
                    AND TRUNC(esa.work_date) = TRUNC(SYSDATE)
                    AND TO_CHAR(SYSDATE, 'HH24:MI:SS')
                        BETWEEN TO_CHAR(s.start_time, 'HH24:MI:SS')
                            AND TO_CHAR(s.end_time, 'HH24:MI:SS')
                )
                OR
                (
                    TO_CHAR(s.start_time, 'HH24:MI:SS') > TO_CHAR(s.end_time, 'HH24:MI:SS')
                    AND (
                            (
                                TRUNC(esa.work_date) = TRUNC(SYSDATE)
                                AND TO_CHAR(SYSDATE, 'HH24:MI:SS') >= TO_CHAR(s.start_time, 'HH24:MI:SS')
                            )
                            OR
                            (
                                TRUNC(esa.work_date) = TRUNC(SYSDATE) - 1
                                AND TO_CHAR(SYSDATE, 'HH24:MI:SS') <= TO_CHAR(s.end_time, 'HH24:MI:SS')
                            )
                        )
                )
          )
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("valid_shift_count") > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
