package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.LoginHistorySql;
import business.sql.rbac.TokenSql;
import common.utils.PasswordUtils;
import model.account.Account;
import model.account.Token;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 * LoginService (BCrypt-only)
 *
 * - Chỉ chấp nhận password lưu trong DB là BCrypt hợp lệ. - Nếu hash bị sửa /
 * xóa / sai format -> đăng nhập thất bại. - Không fallback plain text, không
 * auto-upgrade.
 *
 * Bổ sung: - Sau khi login thành công, load store scope cho user hiện tại. -
 * Nếu user là Store Manager nhưng chưa có store_id thì chặn đăng nhập.
 */
public class LoginService {

    private static final String LOGIN_VERSION = "BCRYPT_ONLY_V5_2026-04-18";

    public static Account authenticate(String username, String password) {
        System.out.println("[" + LOGIN_VERSION + "] authenticate called, username=" + username);

        if (username == null || username.isBlank() || password == null) {
            System.out.println("[" + LOGIN_VERSION + "] FAIL: invalid input");
            return null;
        }

        AccountSql accountSql = AccountSql.getInstance();
        Account acc = accountSql.selectByUsername(username);

        if (acc == null) {
            LoginHistorySql.getInstance().log(
                    null,
                    "LOGIN_FAILED",
                    "FAILURE",
                    "ACCOUNT_NOT_FOUND",
                    localIp(),
                    deviceInfo()
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: ACCOUNT_NOT_FOUND");
            return null;
        }

        String storedHash = acc.getPassword();

        // Debug: không in toàn bộ hash, chỉ in độ dài và prefix ngắn
        if (storedHash == null) {
            System.out.println("[" + LOGIN_VERSION + "] storedHash=null");
        } else {
            String prefix = storedHash.length() > 8 ? storedHash.substring(0, 8) : storedHash;
            System.out.println(
                    "[" + LOGIN_VERSION + "] storedHash.len="
                    + storedHash.length()
                    + " prefix="
                    + prefix
            );
        }

        if (storedHash == null || storedHash.isBlank()) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "EMPTY_PASSWORD_HASH",
                    localIp(),
                    deviceInfo()
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: EMPTY_PASSWORD_HASH");
            return null;
        }

        // Bắt buộc hash đúng chuẩn BCrypt
        boolean isBcrypt = PasswordUtils.isBCryptHash(storedHash);
        System.out.println("[" + LOGIN_VERSION + "] isBCrypt=" + isBcrypt);

        if (!isBcrypt) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "INVALID_PASSWORD_HASH",
                    localIp(),
                    deviceInfo()
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: INVALID_PASSWORD_HASH (manual tampering?)");
            return null;
        }

        // Verify password bằng BCrypt
        final boolean ok;

        try {
            ok = PasswordUtils.checkPassword(password, storedHash);
        } catch (Exception ex) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "BCRYPT_VERIFY_ERROR",
                    localIp(),
                    deviceInfo()
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: BCRYPT_VERIFY_ERROR - " + ex.getMessage());
            return null;
        }

        if (!ok) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "WRONG_PASSWORD",
                    localIp(),
                    deviceInfo()
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: WRONG_PASSWORD");
            return null;
        }

        // =========================================================
        // 1. Tạo token đăng nhập
        // =========================================================
        String tokenValue = UUID.randomUUID().toString();
        acc.setToken(tokenValue);

        Token token = new Token();
        token.setTokenId(UUID.randomUUID().toString());
        token.setAccountId(acc.getAccountId());
        token.setTokenValue(tokenValue);
        token.setExpiryDate(
                new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(45))
        );

        int inserted = TokenSql.getInstance().insert(token);

        if (inserted <= 0) {
            System.out.println("[" + LOGIN_VERSION + "] WARN: token insert failed");

            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "TOKEN_INSERT_FAILED",
                    localIp(),
                    deviceInfo()
            );

            return null;
        }

        System.out.println("[" + LOGIN_VERSION + "] token saved (rows=" + inserted + ")");

        // =========================================================
        // 2. Lưu session RAM
        // =========================================================
        String sessionId = UUID.randomUUID().toString();
        SessionManager.startSession(acc, tokenValue, sessionId);

        // =========================================================
        // 3. Load scope chi nhánh của user đăng nhập
        // =========================================================
        SessionScopeService.loadEmployeeStoreScope(acc);

        System.out.println(
                "[" + LOGIN_VERSION + "] session scope: "
                + "role=" + acc.getRole()
                + ", employeeId=" + SessionManager.getCurrentEmployeeId()
                + ", storeId=" + SessionManager.getCurrentStoreId()
                + ", storeName=" + SessionManager.getCurrentStoreName()
        );

        // =========================================================
        // 4. Chặn Store Manager chưa được phân chi nhánh
        // =========================================================
        if (SessionManager.isStoreManager() && !SessionManager.hasStoreScope()) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(),
                    "LOGIN_FAILED",
                    "FAILURE",
                    "MANAGER_WITHOUT_STORE",
                    localIp(),
                    deviceInfo()
            );

            TokenSql.getInstance().revokeToken(tokenValue);
            SessionManager.clear();

            JOptionPane.showMessageDialog(
                    null,
                    "Tài khoản quản lý chưa được phân chi nhánh.\nVui lòng liên hệ Admin.",
                    "Chưa phân chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );

            System.out.println("[" + LOGIN_VERSION + "] FAIL: MANAGER_WITHOUT_STORE");
            return null;
        }

        // =========================================================
        // 5. Login hợp lệ -> cập nhật trạng thái tài khoản/session
        // =========================================================
        AccountService.onLoginSuccessByRole(acc, sessionId);

        HeartbeatService.start(
                acc.getAccountId(),
                sessionId
        );

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

        if (currentUser != null
                && currentUser.getAccountId() != null
                && !currentUser.getAccountId().isBlank()) {

            String accountId = currentUser.getAccountId();

            // 1. Dừng luồng ping ngầm
            HeartbeatService.stop();

            // 2. Trừ active_sessions trong database
            String sessionId = SessionManager.getCurrentSessionId();

            if (HeartbeatService.markLogoutOnce()) {
                HeartbeatService.stop();

                AccountService.onLogoutOrCloseApp(
                        currentUser.getAccountId(),
                        sessionId
                );
            }

            // 3. Ghi lịch sử đăng xuất
            LoginHistorySql.getInstance().log(
                    accountId,
                    "LOGOUT",
                    "SUCCESS",
                    null,
                    localIp(),
                    deviceInfo()
            );
        }

        // 4. Thu hồi token
        if (currentToken != null && !currentToken.isBlank()) {
            TokenSql.getInstance().revokeToken(currentToken);
        }

        // 5. Dọn session local
        SessionManager.clear();

        System.out.println("[" + LOGIN_VERSION + "] user logged out");
    }

    private static String localIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String deviceInfo() {
        return System.getProperty("os.name")
                + " | Java "
                + System.getProperty("java.version");
    }
}
