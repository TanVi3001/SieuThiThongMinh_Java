package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.LoginHistorySql;
import business.sql.rbac.TokenSql;
import common.utils.PasswordUtils;
import common.db.DatabaseConnection;
import model.account.Account;
import model.account.Token;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 * LoginService (BCrypt-only)
 *
 * - Chỉ chấp nhận password lưu trong DB là BCrypt hợp lệ. - Nếu hash bị sửa /
 * xóa / sai format -> đăng nhập thất bại. - Sau login thành công phải load
 * currentEmployeeId/currentStoreId/currentStoreName. - Admin không bị giới hạn
 * store. - Manager/Staff không có store_id bị chặn vào Store Portal.
 */
public class LoginService {

    private static final String LOGIN_VERSION = "BCRYPT_ONLY_V8_STORE_SCOPE_STAFF_SHIFT_GUARD_2026-05-24";
    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.login");

    public static Account authenticate(String username, String password) {
        debug("[" + LOGIN_VERSION + "] authenticate called, username=" + username);

        if (username == null || username.isBlank() || password == null) {
            debug("[" + LOGIN_VERSION + "] FAIL: invalid input");
            return null;
        }

        AccountSql accountSql = AccountSql.getInstance();
        Account acc = accountSql.selectByUsername(username);

        if (acc == null) {
            LoginHistorySql.getInstance().log(null, "LOGIN_FAILED", "FAILURE", "ACCOUNT_NOT_FOUND", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] FAIL: ACCOUNT_NOT_FOUND");
            return null;
        }

        String storedHash = acc.getPassword();
        if (storedHash == null) {
            debug("[" + LOGIN_VERSION + "] storedHash=null");
        } else {
            String prefix = storedHash.length() > 8 ? storedHash.substring(0, 8) : storedHash;
            debug("[" + LOGIN_VERSION + "] storedHash.len=" + storedHash.length() + " prefix=" + prefix);
        }

        if (storedHash == null || storedHash.isBlank()) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "EMPTY_PASSWORD_HASH", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] FAIL: EMPTY_PASSWORD_HASH");
            return null;
        }

        boolean isBcrypt = PasswordUtils.isBCryptHash(storedHash);
        debug("[" + LOGIN_VERSION + "] isBCrypt=" + isBcrypt);

        if (!isBcrypt) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "INVALID_PASSWORD_HASH", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] FAIL: INVALID_PASSWORD_HASH");
            return null;
        }

        final boolean ok;
        try {
            ok = PasswordUtils.checkPassword(password, storedHash);
        } catch (Exception ex) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "BCRYPT_VERIFY_ERROR", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] FAIL: BCRYPT_VERIFY_ERROR - " + ex.getMessage());
            return null;
        }

        if (!ok) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "WRONG_PASSWORD", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] FAIL: WRONG_PASSWORD");
            return null;
        }

        // =========================================================
        // SHIFT GUARD: Chỉ chặn nhân viên đăng nhập ngoài ca làm việc.
        // Đặt sau khi password đúng, trước khi tạo token/session.
        // Admin và Manager được bỏ qua để luôn vào được hệ thống quản lý/phân ca.
        // Staff muốn vào mọi lúc để test thì gán SHIFT_FULLTIME.
        // =========================================================
        if (!canLoginByCurrentShift(acc)) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "SHIFT_NOT_ALLOWED",
                    localIp(),
                    deviceInfo()
            );

            JOptionPane.showMessageDialog(
                    null,
                    "Tài khoản chưa tới ca làm việc hoặc không có lịch làm việc hợp lệ ở thời điểm hiện tại.\n"
                    + "Vui lòng liên hệ quản lý để kiểm tra phân ca.",
                    "Không được phép đăng nhập",
                    JOptionPane.WARNING_MESSAGE
            );

            debug("[" + LOGIN_VERSION + "] FAIL: SHIFT_NOT_ALLOWED accountId=" + acc.getAccountId());
            return null;
        }

        String tokenValue = UUID.randomUUID().toString();
        acc.setToken(tokenValue);

        Token token = new Token();
        token.setTokenId(UUID.randomUUID().toString());
        token.setAccountId(acc.getAccountId());
        token.setTokenValue(tokenValue);
        token.setExpiryDate(new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(45)));

        int inserted = TokenSql.getInstance().insert(token);
        if (inserted <= 0) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "TOKEN_INSERT_FAILED", localIp(), deviceInfo());
            debug("[" + LOGIN_VERSION + "] WARN: token insert failed");
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        SessionManager.startSession(acc, tokenValue, sessionId);
        business.sql.rbac.AccountSql.getInstance()
                .createLoginSession(acc.getAccountId(), sessionId);
        SessionScopeService.loadEmployeeStoreScope(acc);
        SessionManager.debugPrintScope(LOGIN_VERSION);

        if (SessionManager.isStoreScopedUser()
                && !AccountSql.getInstance().isAccountStoreActive(acc.getAccountId())) {

            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "STORE_INACTIVE",
                    localIp(),
                    deviceInfo()
            );

            TokenSql.getInstance().revokeToken(tokenValue);
            AccountSql.getInstance().closeLoginSession(acc.getAccountId(), sessionId);
            SessionManager.clear();

            JOptionPane.showMessageDialog(
                    null,
                    "Chi nhánh của tài khoản đang tạm ngưng hoạt động.\n"
                    + "Vui lòng liên hệ Admin hoặc chờ chi nhánh được mở lại.",
                    "Chi nhánh tạm ngưng",
                    JOptionPane.WARNING_MESSAGE
            );

            debug("[" + LOGIN_VERSION + "] FAIL: STORE_INACTIVE");
            return null;
        }

        if (SessionManager.isStoreScopedUser() && !SessionManager.hasStoreScope()) {
            LoginHistorySql.getInstance().log(acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "STORE_USER_WITHOUT_STORE", localIp(), deviceInfo());
            TokenSql.getInstance().revokeToken(tokenValue);
            SessionManager.clear();

            JOptionPane.showMessageDialog(
                    null,
                    "Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                    "Chưa phân chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );

            debug("[" + LOGIN_VERSION + "] FAIL: STORE_USER_WITHOUT_STORE");
            return null;
        }

        AccountService.onLoginSuccessByRole(acc, sessionId);
        HeartbeatService.start(acc.getAccountId(), sessionId);
        return acc;
    }

    public static Account getCurrentUser() {
        return SessionManager.getCurrentUser();
    }

    public static String getToken() {
        return SessionManager.getToken();
    }

    public static void logout() {
        Account currentUser = SessionManager.getCurrentUser();
        String currentToken = SessionManager.getToken();

        if (currentUser != null && currentUser.getAccountId() != null && !currentUser.getAccountId().isBlank()) {
            String accountId = currentUser.getAccountId();
            HeartbeatService.stop();
            String sessionId = SessionManager.getCurrentSessionId();

            if (HeartbeatService.markLogoutOnce()) {
                HeartbeatService.stop();
                AccountService.onLogoutOrCloseApp(currentUser.getAccountId(), sessionId);
            }

            LoginHistorySql.getInstance().log(accountId, "LOGOUT", "SUCCESS", null, localIp(), deviceInfo());
        }

        if (currentToken != null && !currentToken.isBlank()) {
            TokenSql.getInstance().revokeToken(currentToken);
        }

        SessionManager.clear();
        debug("[" + LOGIN_VERSION + "] user logged out");
    }

    /**
     * Kiểm tra tài khoản có được đăng nhập ở thời điểm hiện tại theo lịch phân
     * ca không.
     *
     * Bảng cần có: - SHIFTS(shift_id, shift_name, start_time, end_time,
     * is_deleted) - EMPLOYEE_SHIFT_ASSIGNMENTS(assignment_id, employee_id,
     * shift_id, work_date, status, is_deleted)
     *
     * Lưu ý: - Admin/Manager bypass để tránh khóa hệ thống và vẫn vào được màn
     * quản lý phân ca. - SHIFT_FULLTIME dùng cho tài khoản staff test/demo hoặc
     * tài khoản cần login cả ngày. - Ca qua ngày như 23:00 -> 07:00 được xử lý
     * bằng work_date hôm nay hoặc hôm qua.
     */
    private static boolean canLoginByCurrentShift(Account acc) {
        if (acc == null || acc.getAccountId() == null || acc.getAccountId().isBlank()) {
            return false;
        }

        String role = acc.getRole();
        if (isShiftBypassRole(role)) {
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
              AND UPPER(TRIM(TO_CHAR(esa.status))) = 'ASSIGNED'
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
            ps.setString(1, acc.getAccountId());

            try (ResultSet rs = ps.executeQuery()) {
                boolean allowed = rs.next() && rs.getInt("valid_shift_count") > 0;

                debug("[" + LOGIN_VERSION + "] shiftGuard accountId=" + acc.getAccountId()
                        + ", role=" + role
                        + ", allowed=" + allowed);

                return allowed;
            }

        } catch (Exception ex) {
            // Fail-closed cho nhân viên: nếu bảng phân ca/query lỗi thì không cho login để tránh bypass.
            // Admin/Manager đã được bypass ở trên.
            debug("[" + LOGIN_VERSION + "] FAIL: SHIFT_CHECK_ERROR accountId="
                    + acc.getAccountId() + " - " + ex.getMessage());
            return false;
        }
    }

    private static boolean isShiftBypassRole(String role) {
        if (role == null) {
            return false;
        }

        String r = role.trim().toUpperCase();

        return r.equals("R_ADMIN_ALL")
                || r.equals("ADMIN")
                || r.equals("R_ADMIN")
                || r.equals("R_SYSTEM_ADMIN")
                || r.equals("R_CENTRAL_ADMIN")
                // Store Manager / Manager luôn được đăng nhập để quản lý nhân viên và phân ca.
                || r.equals("R_STORE_MNG")
                || r.equals("R_STORE_MANAGER")
                || r.equals("STORE_MANAGER")
                || r.equals("MANAGER");
    }

    private static String localIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String deviceInfo() {
        return System.getProperty("os.name") + " | Java " + System.getProperty("java.version");
    }

    private static void debug(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }
}
