package business.service;

import java.text.Normalizer;
import java.util.Locale;
import model.account.Account;

public final class AuthorizationService {

    private AuthorizationService() {
    }

    // =========================================================
    // 1. NHẬN DIỆN CHỨC VỤ (ROLE IDENTIFICATION)
    // =========================================================
    public static boolean isAdmin() {
        return isAdmin(SessionManager.getCurrentUser());
    }

    public static boolean isAdmin(Account account) {
        if (account == null) {
            return false;
        }
        String role = normalize(account.getRole());
        return role.equals("radminall") || role.equals("admin") || role.equals("quantrivien");
    }

    public static boolean isStoreManager() {
        return isStoreManager(SessionManager.getCurrentUser());
    }

    public static boolean isStoreManager(Account account) {
        if (account == null) {
            return false;
        }
        String role = normalize(account.getRole());
        return role.equals("rstoremng") || role.equals("quanlycuahang") || role.equals("manager");
    }

    public static boolean isCashier() {
        return isCashier(SessionManager.getCurrentUser());
    }

    public static boolean isCashier(Account account) {
        if (account == null) {
            return false;
        }
        String role = normalize(account.getRole());
        // Khớp với R_STAFF_SALE hoặc R_CASHIER trong Database
        return role.equals("rcashier") || role.equals("rstaffsale") || role.equals("nhanvienbanhang") || role.equals("thungan");
    }

    public static boolean isWarehouseStaff() {
        return isWarehouseStaff(SessionManager.getCurrentUser());
    }

    public static boolean isWarehouseStaff(Account account) {
        if (account == null) {
            return false;
        }

        String role = normalize(account.getRole());

        return role.equals("rstaffwarehouse")
                || role.equals("rstaffimport")
                || role.equals("rstaffinventory")
                || role.equals("rstaffstock")
                || role.equals("nhanvienkho");
    }

    public static boolean isProductViewer() {
        return isProductViewer(SessionManager.getCurrentUser());
    }

    public static boolean isProductViewer(Account account) {
        if (account == null) {
            return false;
        }

        String role = normalize(account.getRole());

        return role.equals("rstaffviewprod");
    }

    // =========================================================
    // 2. PHÂN QUYỀN HIỂN THỊ SIDEBAR (MODULE ACCESS CONTROL)
    // =========================================================
    // Màn hình Bán hàng (POS) & Khách hàng: Thu ngân, Quản lý, Admin
    public static boolean canAccessPOS() {
        return isCashier() || isStoreManager() || isAdmin();
    }

    // Màn hình Sản phẩm & Nhập kho: Nhân viên kho, Quản lý, Admin
    public static boolean canAccessProductsAndInventory() {
        return isCashier()
                || isWarehouseStaff()
                || isProductViewer()
                || isStoreManager()
                || isAdmin();
    }

    // Màn hình Hóa đơn: Thu ngân (để xem/in lại), Quản lý, Admin
    public static boolean canAccessInvoices() {
        return isCashier() || isStoreManager() || isAdmin();
    }

    // Màn hình Báo cáo Thống kê & Quản lý Nhân sự: Chỉ Quản lý và Admin
    public static boolean canAccessStatisticsAndEmployees() {
        return isStoreManager() || isAdmin();
    }

    // =========================================================
    // 3. TIỆN ÍCH
    // =========================================================
    public static String currentRoleForUi() {
        Account account = SessionManager.getCurrentUser();

        if (isAdmin(account)) {
            return "Quản trị viên";
        }

        if (isStoreManager(account)) {
            return "Quản lý cửa hàng";
        }

        if (isWarehouseStaff(account)) {
            return "Nhân viên kho";
        }

        if (isProductViewer(account)) {
            return "Nhân viên xem sản phẩm";
        }

        if (isCashier(account)) {
            return "Thu ngân";
        }

        return "Nhân viên";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]", "");
    }

    public static boolean canAccessEmployeeManagement() {
        return canAccessStatisticsAndEmployees();
    }

    public static boolean canAccessStatistics() {
        return canAccessStatisticsAndEmployees();
    }
}
