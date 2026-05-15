package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.LoginHistorySql;
import business.sql.rbac.TokenSql;
import common.utils.PasswordUtils;
import model.account.Account;
import model.account.Token;
import business.service.HeartbeatService;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * LoginService (BCrypt-only)
 *
 * - Chỉ chấp nhận password lưu trong DB là BCrypt hợp lệ. - Nếu hash bị sửa /
 * xóa / sai format -> đăng nhập thất bại (không ném exception). - Không
 * fallback plain text, không auto-upgrade.
 *
 * Sau dán file này, làm Clean -> Rebuild -> Stop app cũ -> Run lại để đảm bảo
 * app dùng code mới.
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
                    null, "LOGIN_FAILED", "FAILURE", "ACCOUNT_NOT_FOUND", localIp(), deviceInfo()
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
            System.out.println("[" + LOGIN_VERSION + "] storedHash.len=" + storedHash.length() + " prefix=" + prefix);
        }

        if (storedHash == null || storedHash.isBlank()) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "EMPTY_PASSWORD_HASH", localIp(), deviceInfo()
            );
            System.out.println("[" + LOGIN_VERSION + "] FAIL: EMPTY_PASSWORD_HASH");
            return null;
        }

        // Bắt buộc hash đúng chuẩn BCrypt, nếu không -> fail (đây là requirement)
        boolean isBcrypt = PasswordUtils.isBCryptHash(storedHash);
        System.out.println("[" + LOGIN_VERSION + "] isBCrypt=" + isBcrypt);
        if (!isBcrypt) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "INVALID_PASSWORD_HASH", localIp(), deviceInfo()
            );
            System.out.println("[" + LOGIN_VERSION + "] FAIL: INVALID_PASSWORD_HASH (manual tampering?)");
            return null;
        }

        // Verify bằng BCrypt.checkpw (bên trong PasswordUtils đã bọc try/catch)
        final boolean ok;
        try {
            ok = PasswordUtils.checkPassword(password, storedHash);
        } catch (Exception ex) {
            // Phòng thủ: nếu verify ném bất kỳ lỗi runtime nào thì fail
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "BCRYPT_VERIFY_ERROR", localIp(), deviceInfo()
            );
            System.out.println("[" + LOGIN_VERSION + "] FAIL: BCRYPT_VERIFY_ERROR - " + ex.getMessage());
            return null;
        }

        if (!ok) {
            LoginHistorySql.getInstance().log(
                    acc.getAccountId(), "LOGIN_FAILED", "FAILURE", "WRONG_PASSWORD", localIp(), deviceInfo()
            );
            System.out.println("[" + LOGIN_VERSION + "] FAIL: WRONG_PASSWORD");
            return null;
        }

        // Tạo token và lưu DB
        String tokenValue = UUID.randomUUID().toString();
        acc.setToken(tokenValue);

        Token token = new Token();
        token.setTokenId(UUID.randomUUID().toString());
        token.setAccountId(acc.getAccountId());
        token.setTokenValue(tokenValue);
        token.setExpiryDate(new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(45))); // sau 45 phút thì hết hạn token

        int inserted = TokenSql.getInstance().insert(token);
        if (inserted <= 0) {
            System.out.println("[" + LOGIN_VERSION + "] WARN: token insert failed");
            return null;
        } else {
            System.out.println("[" + LOGIN_VERSION + "] token saved (rows=" + inserted + ")");
        }

        // Lưu session RAM
        String sessionId = java.util.UUID.randomUUID().toString();

        SessionManager.startSession(acc, tokenValue, sessionId);

        AccountService.onLoginSuccessByRole(
                acc,
                sessionId
        );

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

        // Xử lý các tác vụ liên quan đến User ID
        if (currentUser != null && currentUser.getAccountId() != null && !currentUser.getAccountId().isBlank()) {
            String accountId = currentUser.getAccountId();

            // 1. Dừng luồng Ping ngầm
            HeartbeatService.stop();

            // 2. Trừ active_sessions trong Database (Thay thế hoàn toàn cho setOffline)
            String sessionId = business.service.SessionManager.getCurrentSessionId();

            if (business.service.HeartbeatService.markLogoutOnce()) {
                business.service.HeartbeatService.stop();

                business.service.AccountService.onLogoutOrCloseApp(
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

        // 4. Thu hồi Token (đặt riêng biệt phòng trường hợp có token nhưng user bị null)
        if (currentToken != null && !currentToken.isBlank()) {
            TokenSql.getInstance().revokeToken(currentToken);
        }

        // 5. Dọn dẹp dữ liệu cục bộ trên máy
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
        return System.getProperty("os.name") + " | Java " + System.getProperty("java.version");
    }
}
