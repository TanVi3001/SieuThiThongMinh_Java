package common.utils;

import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility xử lý password bằng BCrypt. - Hỗ trợ tương thích ngược:
 * hash/hashPassword, verify/checkPassword - Nếu hash bị sửa/hỏng format ->
 * verify/checkPassword trả false
 */
public final class PasswordUtils {

    private static final int LOG_ROUNDS = 10;

    // BCrypt chuẩn: $2a$ / $2b$ / $2y$, cost 2 chữ số, tổng 60 ký tự
    private static final Pattern BCRYPT_PATTERN
            = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordUtils() {
        // Utility class
    }

    /**
     * Băm mật khẩu (tên method chuẩn mới)
     */
    public static String hashPassword(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Password không được null hoặc rỗng");
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Alias tương thích code cũ: PasswordUtils.hash(...)
     */
    public static String hash(String plainText) {
        return hashPassword(plainText);
    }

    /**
     * Kiểm tra chuỗi có đúng format BCrypt không.
     */
    public static boolean isBCryptHash(String hashed) {
        return hashed != null && BCRYPT_PATTERN.matcher(hashed).matches();
    }

    /**
     * So khớp mật khẩu (tên method chuẩn mới)
     */
    public static boolean checkPassword(String plainText, String hashed) {
        if (plainText == null || plainText.isBlank() || hashed == null || hashed.isBlank()) {
            return false;
        }

        // Nếu hash bị sửa/xóa hỏng format -> fail luôn
        if (!isBCryptHash(hashed)) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (IllegalArgumentException ex) {
            // Hash không hợp lệ / bị hỏng
            return false;
        } catch (Exception ex) {
            // Phòng thủ thêm
            return false;
        }
    }

    /**
     * Alias tương thích code cũ: PasswordUtils.verify(...)
     * @param plainText
     * @param hashed
     * @return 
     */
    public static boolean verify(String plainText, String hashed) {
        return checkPassword(plainText, hashed);
    }
    
    
}
