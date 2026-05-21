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
        token = tk;
        currentToken = tk;
    }

    public static void startSession(Account user, String token, String sessionId) {
        currentUser = user;
        SessionManager.token = token;
        currentToken = token;
        currentSessionId = sessionId;
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static String getToken() {
        return token != null ? token : currentToken;
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
    }

    public static String getCurrentEmployeeId() {
        return currentEmployeeId;
    }

    public static String getCurrentStoreId() {
        return currentStoreId;
    }

    public static String getCurrentStoreName() {
        return currentStoreName;
    }

    public static String getCurrentRole() {
        return currentUser != null ? normalize(currentUser.getRole()) : null;
    }

    public static boolean isAdmin() {
        return "R_ADMIN_ALL".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isStoreManager() {
        return "R_STORE_MNG".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isStoreStaff() {
        String role = getCurrentRole();
        return "R_STAFF".equalsIgnoreCase(role)
                || "R_STAFF_SALE".equalsIgnoreCase(role)
                || "R_STAFF_WAREHOUSE".equalsIgnoreCase(role);
    }

    public static boolean isStoreScopedUser() {
        return !isAdmin() && (isStoreManager() || isStoreStaff());
    }

    public static boolean hasStoreScope() {
        return currentStoreId != null && !currentStoreId.isBlank();
    }

    public static String requireCurrentStoreId() {
        if (!hasStoreScope()) {
            throw new IllegalStateException("Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.");
        }
        return currentStoreId;
    }

    public static void debugPrintScope(String source) {
        System.out.println("[SESSION_SCOPE] " + source
                + " currentRole=" + getCurrentRole()
                + ", currentEmployeeId=" + currentEmployeeId
                + ", currentStoreId=" + currentStoreId
                + ", currentStoreName=" + currentStoreName);
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
