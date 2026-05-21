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
    }

    public static void startSession(Account user, String token, String sessionId) {
        currentUser = user;
        currentToken = token;
        currentSessionId = sessionId;
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static String getToken() {
        return token;
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static boolean isLoggedIn() {
        return currentUser != null && token != null && !token.isBlank();
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
        currentEmployeeId = employeeId;
        currentStoreId = storeId;
        currentStoreName = storeName;
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

    public static boolean isAdmin() {
        return currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().equalsIgnoreCase("R_ADMIN_ALL");
    }

    public static boolean isStoreManager() {
        return currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().equalsIgnoreCase("R_STORE_MNG");
    }

    public static boolean hasStoreScope() {
        return currentStoreId != null && !currentStoreId.isBlank();
    }
}
