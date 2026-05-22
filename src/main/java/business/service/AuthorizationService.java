package business.service;

import model.account.Account;

public class AuthorizationService {

    private AuthorizationService() {
    }

    /*
     * =========================================================
     * 4 ROLE CHÍNH CỦA ĐỒ ÁN
     * =========================================================
     *
     * R_ADMIN_ALL        = Admin toàn hệ thống
     * R_STORE_MNG        = Manager / quản lý chi nhánh
     * R_STAFF_SALE       = Nhân viên bán hàng
     * R_STAFF_VIEW_PROD  = Staff Product / nhân viên sản phẩm-kho-nhập hàng
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

    /**
     * Trong đồ án này: R_STAFF_VIEW_PROD = Staff Product = nhân viên sản
     * phẩm/kho/nhập hàng. Không hiểu là view-only.
     */
    public static boolean isProductStaff() {
        return isProductStaff(SessionManager.getCurrentUser());
    }

    public static boolean isProductStaff(Account account) {
        return hasRole(account, "R_STAFF_VIEW_PROD");
    }

    /**
     * Giữ tên cũ để các file đang gọi isWarehouseStaff() không bị lỗi compile.
     * Ý nghĩa thật hiện tại: Warehouse Staff chính là Staff Product.
     */
    public static boolean isWarehouseStaff() {
        return isProductStaff();
    }

    public static boolean isWarehouseStaff(Account account) {
        return isProductStaff(account);
    }

    /**
     * Các user bị giới hạn theo store_id: - Manager - Staff Sale - Staff
     * Product
     *
     * Admin không bị scope.
     */
    public static boolean isStoreScopedUser() {
        return isStoreScopedUser(SessionManager.getCurrentUser());
    }

    public static boolean isStoreScopedUser(Account account) {
        return !isAdmin(account)
                && (isStoreManager(account) || isCashier(account) || isProductStaff(account));
    }

    /*
     * =========================================================
     * QUYỀN THEO MODULE
     * =========================================================
     */
    /**
     * Module bán hàng/POS: Admin, Manager, Staff Sale được vào.
     */
    public static boolean canAccessSales() {
        return isAdmin() || isStoreManager() || isCashier();
    }

    /**
     * Module sản phẩm/tồn kho: Admin, Manager, Staff Product được vào.
     */
    public static boolean canAccessProductsAndInventory() {
        return isAdmin() || isStoreManager() || isProductStaff();
    }

    /**
     * Nhập kho/import CSV/cập nhật tồn: Admin, Manager, Staff Product được thao
     * tác.
     */
    public static boolean canManageStock() {
        return isAdmin() || isStoreManager() || isProductStaff();
    }

    /**
     * Module khách hàng: Admin, Manager, Staff Sale được vào. Khách hàng vẫn là
     * global để tra SĐT/voucher, nhưng dữ liệu chi tiêu/order vẫn phải tính
     * theo store_id ở tầng SQL/service.
     */
    public static boolean canAccessCustomers() {
        return isAdmin() || isStoreManager() || isCashier();
    }

    /**
     * Module hóa đơn: Admin, Manager, Staff Sale được vào.
     */
    public static boolean canAccessOrders() {
        return isAdmin() || isStoreManager() || isCashier();
    }

    /**
     * Module nhân viên: Admin xem toàn hệ thống, Manager xem nhân viên cùng
     * store.
     */
    public static boolean canAccessEmployees() {
        return isAdmin() || isStoreManager();
    }

    /**
     * Module nhà cung cấp/danh mục/thuế: Admin, Manager, Staff Product được
     * vào.
     */
    public static boolean canAccessSupplierAndCategory() {
        return isAdmin() || isStoreManager() || isProductStaff();
    }

    /**
     * Báo cáo/thống kê: Admin toàn hệ thống, Manager theo chi nhánh.
     */
    public static boolean canAccessReports() {
        return isAdmin() || isStoreManager();
    }

    /**
     * Quản trị hệ thống: Chỉ Admin.
     */
    public static boolean canAccessAdminPanel() {
        return isAdmin();
    }

    /**
     * Quản lý phân quyền/tài khoản: Chỉ Admin.
     */
    public static boolean canManageAccountsAndRoles() {
        return isAdmin();
    }

    /**
     * Quản lý cửa hàng/chi nhánh: Chỉ Admin.
     */
    public static boolean canManageStores() {
        return isAdmin();
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

    /*
     * =========================================================
     * INTERNAL HELPER
     * =========================================================
     */
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

        return null;
    }

    /**
     * R_STAFF_VIEW_PROD -> rstaffviewprod R-STAFF-VIEW-PROD -> rstaffviewprod
     */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
