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
        clearScopeOnly();
    }

    public static void startSession(Account user, String tk, String sessionId) {
        currentUser = user;
        token = normalize(tk);
        currentToken = normalize(tk);
        currentSessionId = normalize(sessionId);
        clearScopeOnly();
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
        clearScopeOnly();
    }

    private static void clearScopeOnly() {
        currentEmployeeId = null;
        currentStoreId = null;
        currentStoreName = null;
    }

    public static void setCurrentEmployeeScope(String employeeId, String storeId, String storeName) {
        currentEmployeeId = normalize(employeeId);
        currentStoreId = normalize(storeId);
        currentStoreName = normalize(storeName);

        try {
            if (currentUser != null && isBlank(currentUser.getUserId()) && currentEmployeeId != null) {
                currentUser.setUserId(currentEmployeeId);
            }
        } catch (Exception ignored) {
        }
    }

    public static String getCurrentEmployeeId() {
        if (!isBlank(currentEmployeeId)) {
            return currentEmployeeId;
        }

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

        String role = normalize(currentUser.getRoleId());
        if (role == null) {
            role = normalize(currentUser.getRole());
        }
        if (role == null) {
            role = normalize(currentUser.getRoleValue());
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

    public static boolean isProductStaff() {
        return "R_STAFF_VIEW_PROD".equalsIgnoreCase(getCurrentRole());
    }

    public static boolean isWarehouseStaff() {
        return isProductStaff();
    }

    public static boolean isStoreStaff() {
        return isSaleStaff() || isProductStaff();
    }

    public static boolean isStoreScopedUser() {
        return !isAdmin() && (isStoreManager() || isStoreStaff());
    }

    public static boolean hasStoreScope() {
        return currentStoreId != null && !currentStoreId.isBlank();
    }

    public static String getScopedStoreIdOrNull() {
        if (isAdmin()) {
            return null;
        }
        return hasStoreScope() ? currentStoreId : null;
    }

    public static String requireCurrentStoreId() {
        if (!hasStoreScope()) {
            throw new IllegalStateException("Tài khoản chưa được phân chi nhánh. Vui lòng liên hệ Admin.");
        }
        return currentStoreId;
    }

    public static String requireCurrentEmployeeId() {
        String employeeId = getCurrentEmployeeId();
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalStateException("Không xác định được nhân viên hiện tại. Vui lòng đăng nhập lại.");
        }
        return employeeId;
    }

    // Helpers theo nghiệp vụ hiện tại. AuthorizationService là nguồn chính,
    // các hàm này giữ để code cũ không lỗi compile.
    public static boolean canManageStock() {
        return isAdmin() || isProductStaff();
    }

    public static boolean canAccessSales() {
        return isAdmin() || isSaleStaff();
    }

    public static boolean canAccessProductsAndInventory() {
        return isAdmin() || isStoreManager() || isSaleStaff() || isProductStaff();
    }

    public static boolean canAccessCustomers() {
        return isAdmin() || isStoreManager() || isSaleStaff();
    }

    public static boolean canAccessOrders() {
        return isAdmin() || isStoreManager() || isSaleStaff();
    }

    public static boolean canAccessEmployees() {
        return isAdmin() || isStoreManager();
    }

    public static boolean canAccessSupplierAndCategory() {
        return isAdmin() || isStoreManager() || isProductStaff();
    }

    public static String getCurrentRoleLabel() {
        if (isAdmin()) {
            return "Admin";
        }
        if (isStoreManager()) {
            return "Manager";
        }
        if (isSaleStaff()) {
            return "Staff Sale";
        }
        if (isProductStaff()) {
            return "Staff Product";
        }
        return "Unknown";
    }

    public static String getCurrentPortalTitle() {
        if (isAdmin()) {
            return "SMART SUPERMARKET - CENTRAL ADMIN PORTAL";
        }
        if (isStoreManager()) {
            return "SMART SUPERMARKET - STORE PORTAL";
        }
        if (isSaleStaff()) {
            return "SMART SUPERMARKET - SALE PORTAL";
        }
        if (isProductStaff()) {
            return "SMART SUPERMARKET - WAREHOUSE PORTAL";
        }
        return "SMART SUPERMARKET";
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
                + ", roleLabel=" + getCurrentRoleLabel()
                + ", currentEmployeeId=" + currentEmployeeId
                + ", effectiveEmployeeId=" + getCurrentEmployeeId()
                + ", currentStoreId=" + currentStoreId
                + ", currentStoreName=" + currentStoreName
                + ", scopeLabel=" + getScopeLabel());
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
