package business.service;

import model.account.Account;

public class AuthorizationService {

    private AuthorizationService() {
    }

    /*
     * =========================================================
     * 4 ROLE CHÍNH CỦA ĐỒ ÁN
     * =========================================================
     */
    public static boolean isAdmin() {
        return isAdmin(SessionManager.getCurrentUser());
    }

    public static boolean isAdmin(Account account) {
        return hasRole(account, "R_ADMIN_ALL");
    }

    public static boolean isStoreManager() {
        return isStoreManager(SessionManager.getCurrentUser());
    }

    public static boolean isStoreManager(Account account) {
        return hasRole(account, "R_STORE_MNG");
    }

    public static boolean isCashier() {
        return isCashier(SessionManager.getCurrentUser());
    }

    public static boolean isCashier(Account account) {
        return hasRole(account, "R_STAFF_SALE");
    }

    public static boolean isSaleStaff() {
        return isCashier();
    }

    public static boolean isSaleStaff(Account account) {
        return isCashier(account);
    }

    public static boolean isProductStaff() {
        return isProductStaff(SessionManager.getCurrentUser());
    }

    public static boolean isProductStaff(Account account) {
        return hasRole(account, "R_STAFF_VIEW_PROD");
    }

    public static boolean isWarehouseStaff() {
        return isProductStaff();
    }

    public static boolean isWarehouseStaff(Account account) {
        return isProductStaff(account);
    }

    public static boolean isStoreScopedUser() {
        return isStoreScopedUser(SessionManager.getCurrentUser());
    }

    public static boolean isStoreScopedUser(Account account) {
        return !isAdmin(account)
                && (isStoreManager(account) || isCashier(account) || isProductStaff(account));
    }

    /*
     * =========================================================
     * QUYỀN MA TRẬN CHUNG
     * =========================================================
     */
    public static boolean canViewByMatrix() {
        return RolePermissionService.canView();
    }

    public static boolean canAddByMatrix() {
        return RolePermissionService.canAdd();
    }

    public static boolean canEditByMatrix() {
        return RolePermissionService.canEdit();
    }

    public static boolean canDeleteByMatrix() {
        return RolePermissionService.canDelete();
    }

    public static boolean canExportByMatrix() {
        return RolePermissionService.canExport();
    }

    private static boolean canViewCurrentRole() {
        return RolePermissionService.canView();
    }

    /*
     * =========================================================
     * QUYỀN THEO MODULE
     * =========================================================
     */
    public static boolean canAccessSales() {
        return canViewCurrentRole() && (isAdmin() || isCashier());
    }

    public static boolean canAccessProductsAndInventory() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager() || isCashier() || isProductStaff());
    }

    public static boolean canManageStock() {
        return canViewCurrentRole() && (isAdmin() || isProductStaff());
    }

    public static boolean canManageProducts() {
        return canViewCurrentRole() && RolePermissionService.canEdit() && (isAdmin() || isProductStaff());
    }

    public static boolean canSendInventoryAlert() {
        return canViewCurrentRole() && (isStoreManager() || isCashier());
    }

    public static boolean canAccessCustomers() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager() || isCashier());
    }

    public static boolean canAccessOrders() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager() || isCashier());
    }

    public static boolean canAccessEmployees() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager());
    }

    public static boolean canAccessSupplierAndCategory() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager() || isProductStaff());
    }

    public static boolean canAccessReports() {
        return canViewCurrentRole() && (isAdmin() || isStoreManager());
    }

    public static boolean canAccessSettings() {
        return canViewCurrentRole();
    }

    public static boolean canAccessDashboard() {
        return canViewCurrentRole();
    }

    public static boolean canAccessStatisticsAndEmployees() {
        return canAccessReports() || canAccessEmployees();
    }

    public static boolean canAccessEmployeeManagement() {
        return canAccessEmployees();
    }

    public static boolean canManageAccountsAndRoles() {
        return canViewCurrentRole() && isAdmin();
    }

    public static boolean canManageStores() {
        return canViewCurrentRole() && isAdmin();
    }

    public static boolean canAccessAdminPanel() {
        return canViewCurrentRole() && isAdmin();
    }

    /*
     * =========================================================
     * UI HELPER
     * =========================================================
     */
    public static String currentRoleForUi() {
        Account account = SessionManager.getCurrentUser();

        if (isAdmin(account)) {
            return "Admin";
        }

        if (isStoreManager(account)) {
            return "Manager";
        }

        if (isCashier(account)) {
            return "Staff Sale";
        }

        if (isProductStaff(account)) {
            return "Staff Product";
        }

        return "Unknown";
    }

    public static String currentPortalTitle() {
        Account account = SessionManager.getCurrentUser();

        if (isAdmin(account)) {
            return "SMART SUPERMARKET - CENTRAL ADMIN PORTAL";
        }

        if (isStoreManager(account)) {
            return "SMART SUPERMARKET - STORE PORTAL";
        }

        if (isCashier(account)) {
            return "SMART SUPERMARKET - SALE PORTAL";
        }

        if (isProductStaff(account)) {
            return "SMART SUPERMARKET - WAREHOUSE PORTAL";
        }

        return "SMART SUPERMARKET";
    }

    public static String getHomePanelName() {
        Account account = SessionManager.getCurrentUser();

        if (isAdmin(account)) {
            return "ADMIN_HOME";
        }

        if (isStoreManager(account)) {
            return "MANAGER_HOME";
        }

        if (isCashier(account)) {
            return "SALE_HOME";
        }

        if (isProductStaff(account)) {
            return "WAREHOUSE_HOME";
        }

        return "UNKNOWN_HOME";
    }

    private static boolean hasRole(Account account, String expectedRole) {
        if (account == null || expectedRole == null) {
            return false;
        }

        String role = normalize(readRole(account));
        String expected = normalize(expectedRole);

        return expected.equals(role);
    }

    private static String readRole(Account account) {
        if (account == null) {
            return null;
        }

        try {
            String role = account.getRole();
            if (role != null && !role.trim().isEmpty()) {
                return role.trim();
            }
        } catch (Exception ignored) {
        }

        try {
            String roleId = account.getRoleId();
            if (roleId != null && !roleId.trim().isEmpty()) {
                return roleId.trim();
            }
        } catch (Exception ignored) {
        }

        try {
            String roleValue = account.getRoleValue();
            if (roleValue != null && !roleValue.trim().isEmpty()) {
                return roleValue.trim();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
