package business.service;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class RolePermissionService {

    private RolePermissionService() {
    }

    public static boolean canView() {
        return can("can_view");
    }

    public static boolean canAdd() {
        return can("can_add");
    }

    public static boolean canEdit() {
        return can("can_edit");
    }

    public static boolean canDelete() {
        return can("can_delete");
    }

    public static boolean canExport() {
        return can("can_export");
    }

    public static boolean can(String columnName) {
        String roleId = normalizeRoleId(SessionManager.getCurrentRole());

        if (roleId == null) {
            return false;
        }

        if ("R_ADMIN_ALL".equals(roleId)) {
            return true;
        }

        if (!isAllowedColumn(columnName)) {
            return false;
        }

        String sql = "SELECT " + columnName + " AS allowed "
                + "FROM ROLES "
                + "WHERE role_id = ? "
                + "AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roleId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("allowed") == 1;
            }
        } catch (Exception e) {
            System.err.println("[RolePermissionService] Cannot read permission: " + e.getMessage());
            return false;
        }
    }

    private static boolean isAllowedColumn(String columnName) {
        return "can_view".equals(columnName)
                || "can_add".equals(columnName)
                || "can_edit".equals(columnName)
                || "can_delete".equals(columnName)
                || "can_export".equals(columnName);
    }

    private static String normalizeRoleId(String roleId) {
        if (roleId == null || roleId.trim().isEmpty()) {
            return null;
        }

        String role = roleId.trim().toUpperCase();

        if ("R_MANAGER".equals(role)) {
            return "R_STORE_MNG";
        }

        if ("R_STAFF_STOCK".equals(role)) {
            return "R_STAFF_VIEW_PROD";
        }

        return role;
    }
}
