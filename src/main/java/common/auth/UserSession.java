package common.auth;

/**
 * Lưu trữ phiên làm việc của người dùng hiện tại (Singleton) Dùng để kiểm tra
 * quyền và định danh trên toàn hệ thống.
 */
public class UserSession {

    private static UserSession instance;

    private String userId;
    private String fullName;
    private String userRole; // Lưu Role ID (VD: R_ADMIN_ALL, R_STORE_MNG)

    private UserSession() {
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Thiết lập thông tin khi đăng nhập thành công
    public void createUserSession(String userId, String fullName, String userRole) {
        this.userId = userId;
        this.fullName = fullName;
        this.userRole = userRole;
    }

    // Xóa sạch thông tin khi Logout
    public void clear() {
        this.userId = null;
        this.fullName = null;
        this.userRole = null;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUserRole() {
        return userRole;
    }

    // Kiểm tra xem đã đăng nhập chưa
    public boolean isLoggedIn() {
        return userId != null;
    }
}
