package business.service;

import model.account.Account;

public class SessionManager {

    private static String token;
    private static Account currentUser;
    private static String currentToken;
    private static String currentSessionId;

    private static String currentEmployeeId;
    private static String currentStoreId;
    private static String currentStoreName;

    private SessionManager() {
    }

    public static void startSession(Account user, String tk) {
        currentUser = user;
        token = normalize(tk);
        currentToken = normalize(tk);
        currentSessionId = null;

        // Không clear scope ở đây vì LoginService sẽ gọi SessionScopeService ngay sau đó.
        // Nhưng để tránh dính session cũ khi login user khác, clear scope trước.
        currentEmployeeId = null;
        currentStoreId = null;
        currentStoreName = null;
    }

    public static void startSession(Account user, String tk, String sessionId) {
        currentUser = user;
        token = normalize(tk);
        currentToken = normalize(tk);
        currentSessionId = normalize(sessionId);

        // Tránh dính scope của tài khoản đăng nhập trước.
        currentEmployeeId = null;
        currentStoreId = null;
        currentStoreName = null;
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static String getToken() {
        return token != null ? token : currentToken;
    }

    public static String getCurrentToken() {
        return getToken();
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static boolean isLoggedIn() {
        String tk = getToken();
        return currentUser != null && tk != null && !tk.isBlank();
    }

    public static void clear() {
        currentUser = null;
        token = null;
        currentToken = null;
        currentSessionId = null;
        currentEmployeeId = null;
        currentStoreId = null;
        currentStoreName = null;
    }

    public static void setCurrentEmployeeScope(String employeeId, String storeId, String storeName) {
        currentEmployeeId = normalize(employeeId);
        currentStoreId = normalize(storeId);
        currentStoreName = normalize(storeName);

        // Nếu Account chưa có userId nhưng scope load được employeeId thì đồng bộ ngược lại.
        // Không bắt buộc, nhưng giúp các màn cũ dùng currentUser.getUserId() vẫn đúng hơn.
        try {
            if (currentUser != null
                    && isBlank(currentUser.getUserId())
                    && currentEmployeeId != null) {
                currentUser.setUserId(currentEmployeeId);
            }
        } catch (Exception ignored) {
        }
    }

    public static String getCurrentEmployeeId() {
        if (!isBlank(currentEmployeeId)) {
            return currentEmployeeId;
        }

        // Fallback quan trọng:
        // ACCOUNTS.user_id thường chính là EMPLOYEES.employee_id.
        if (currentUser != null && !isBlank(currentUser.getUserId())) {
            return currentUser.getUserId().trim();
        }

        return null;
    }

    public static String getCurrentStoreId() {
        return currentStoreId;
    }

    public static String getCurrentStoreName() {
        return currentStoreName;
    }

    public static String getCurrentRole() {
        if (currentUser == null) {
            return null;
        }

        String role = normalize(currentUser.getRole());

        if (role == null) {
            role = normalize(currentUser.getRoleId());
        }

        return role;
    }

    public static boolean isAdmin() {
        return "R_ADMIN_ALL".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isStoreManager() {
        return "R_STORE_MNG".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isSaleStaff() {
        return "R_STAFF_SALE".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isWarehouseStaff() {
        String role = getCurrentRole();

        return "R_STAFF_WAREHOUSE".equalsIgnoreCase(role)
                || "R_STAFF_IMPORT".equalsIgnoreCase(role)
                || "R_STAFF_INVENTORY".equalsIgnoreCase(role);
    }

    public static boolean isProductViewStaff() {
        return "R_STAFF_VIEW_PROD".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isStoreStaff() {
        String role = getCurrentRole();

        return "R_STAFF".equalsIgnoreCase(role)
                || "R_STAFF_SALE".equalsIgnoreCase(role)
                || "R_STAFF_WAREHOUSE".equalsIgnoreCase(role)
                || "R_STAFF_IMPORT".equalsIgnoreCase(role)
                || "R_STAFF_INVENTORY".equalsIgnoreCase(role)
                || "R_STAFF_VIEW_PROD".equalsIgnoreCase(role);
    }

    public static boolean isStoreScopedUser() {
        return !isAdmin() && (isStoreManager() || isStoreStaff());
    }

    public static boolean hasStoreScope() {
        return currentStoreId != null && !currentStoreId.isBlank();
    }

    /**
     * Dùng cho SQL/service cần filter dữ liệu theo chi nhánh.
     *
     * Admin: return null => xem toàn hệ thống. Manager/Staff: return
     * currentStoreId.
     */
    public static String getScopedStoreIdOrNull() {
        if (isAdmin()) {
            return null;
        }

        return hasStoreScope() ? currentStoreId : null;
    }

    /**
     * Dùng cho Manager/Staff bắt buộc phải có chi nhánh.
     */
    public static String requireCurrentStoreId() {
        if (!hasStoreScope()) {
            throw new IllegalStateException("Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.");
        }

        return currentStoreId;
    }

    /**
     * Dùng cho các màn tạo dữ liệu vận hành như bán hàng, nhập kho, phân ca.
     */
    public static String requireCurrentEmployeeId() {
        String employeeId = getCurrentEmployeeId();

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalStateException("Không xác định được nhân viên hiện tại. Vui lòng đăng nhập lại.");
        }

        return employeeId;
    }

    public static String getScopeLabel() {
        if (isAdmin()) {
            return "Toàn hệ thống";
        }

        if (!isBlank(currentStoreName)) {
            return currentStoreName;
        }

        if (!isBlank(currentStoreId)) {
            return currentStoreId;
        }

        return "Chưa phân chi nhánh";
    }

    public static void debugPrintScope(String source) {
        System.out.println("[SESSION_SCOPE] " + source
                + " accountId=" + safeAccountId()
                + ", userId=" + safeUserId()
                + ", username=" + safeUsername()
                + ", currentRole=" + getCurrentRole()
                + ", currentEmployeeId=" + currentEmployeeId
                + ", effectiveEmployeeId=" + getCurrentEmployeeId()
                + ", currentStoreId=" + currentStoreId
                + ", currentStoreName=" + currentStoreName);
    }

    private static String safeAccountId() {
        try {
            return currentUser == null ? null : currentUser.getAccountId();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeUserId() {
        try {
            return currentUser == null ? null : currentUser.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeUsername() {
        try {
            return currentUser == null ? null : currentUser.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
