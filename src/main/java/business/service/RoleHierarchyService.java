package business.service;

import java.util.*;

public final class RoleHierarchyService {

    private RoleHierarchyService() {
    }

    public static final String ADMIN = "R_ADMIN_ALL";
    public static final String MANAGER = "R_MANAGER";
    public static final String WAREHOUSE = "R_STAFF_VIEW_PROD";
    public static final String SALE = "R_STAFF_SALE";

    private static final Map<String, Integer> LEVEL = new HashMap<>();

    static {
        LEVEL.put(ADMIN, 4);
        LEVEL.put(MANAGER, 3);
        LEVEL.put(WAREHOUSE, 2);
        LEVEL.put(SALE, 1);
    }

    public static int levelOf(String roleId) {
        if (roleId == null) {
            return 0;
        }
        return LEVEL.getOrDefault(roleId.trim().toUpperCase(), 0);
    }

    public static boolean isAdmin(String roleId) {
        return ADMIN.equalsIgnoreCase(roleId);
    }

    public static boolean isManager(String roleId) {
        return MANAGER.equalsIgnoreCase(roleId);
    }

    public static boolean canAccessRoleAssignment(String currentRole) {
        return isAdmin(currentRole) || isManager(currentRole);
    }

    public static boolean canAssignRole(String currentRole, String targetRole) {
        int currentLevel = levelOf(currentRole);
        int targetLevel = levelOf(targetRole);

        if (currentLevel <= 0 || targetLevel <= 0) {
            return false;
        }

        // Admin được gán role thấp hơn Admin
        if (isAdmin(currentRole)) {
            return targetLevel < currentLevel;
        }

        // Manager chỉ được gán Kho hoặc Sale
        if (isManager(currentRole)) {
            return targetLevel < currentLevel;
        }

        return false;
    }

    public static boolean canModifyAccount(
            String currentRole,
            String currentAccountId,
            String currentStoreId,
            String targetAccountId,
            String targetRole,
            String targetStoreId
    ) {
        if (!canAccessRoleAssignment(currentRole)) {
            return false;
        }

        // Không cho tự khóa / tự hạ quyền chính mình ở màn phân quyền
        if (currentAccountId != null && currentAccountId.equalsIgnoreCase(targetAccountId)) {
            return false;
        }

        int currentLevel = levelOf(currentRole);
        int targetLevel = levelOf(targetRole);

        if (currentLevel <= targetLevel) {
            return false;
        }

        if (isAdmin(currentRole)) {
            return true;
        }

        if (isManager(currentRole)) {
            return currentStoreId != null
                    && targetStoreId != null
                    && currentStoreId.equalsIgnoreCase(targetStoreId);
        }

        return false;
    }

    public static List<String> getAssignableRoles(String currentRole) {
        List<String> roles = new ArrayList<>();

        if (isAdmin(currentRole)) {
            roles.add(MANAGER);
            roles.add(WAREHOUSE);
            roles.add(SALE);
        } else if (isManager(currentRole)) {
            roles.add(WAREHOUSE);
            roles.add(SALE);
        }

        return roles;
    }
}
