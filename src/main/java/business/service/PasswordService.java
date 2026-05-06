package business.service;

import business.sql.rbac.AccountSql;
import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * Password helper: check + hash + update DB.
 */
public final class PasswordService {

    private static final int LOG_ROUNDS = 10;

    private static final Pattern BCRYPT_PATTERN
            = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordService() {
        /* utility */
    }

    public static boolean isBCryptHash(String hash) {
        return hash != null && BCRYPT_PATTERN.matcher(hash).matches();
    }

    public static boolean checkPassword(String plainPassword, String hashFromDb) {
        if (plainPassword == null || plainPassword.isBlank()) {
            return false;
        }
        if (hashFromDb == null || hashFromDb.isBlank()) {
            return false;
        }
        if (!isBCryptHash(hashFromDb)) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashFromDb);
        } catch (Exception ex) {
            return false;
        }
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password không được rỗng");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean updatePasswordByAccountId(String accountId, String newPlainPassword) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        if (newPlainPassword == null || newPlainPassword.isBlank()) {
            return false;
        }

        String newHash = hashPassword(newPlainPassword);
        AccountSql accountSql = AccountSql.getInstance();
        return accountSql.updatePasswordByAccountId(accountId, newHash);
    }

    /**
     * Update theo email: HASH ĐÚNG 1 LẦN và update vào DB.
     */
    public static boolean updatePasswordByEmail(String email, String newPlainPassword) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (newPlainPassword == null || newPlainPassword.isBlank()) {
            return false;
        }

        String newHash = hashPassword(newPlainPassword);

        AccountSql accountSql = AccountSql.getInstance();
        // GỌI method update theo HASH (đã sửa ở AccountSql bên dưới)
        return accountSql.updatePasswordHashByEmail(email, newHash);
    }
}
