package business.service;

import common.db.DatabaseConnection;
import model.account.Account;
import java.awt.Window;
import java.sql.*;
import java.util.UUID;
import javax.swing.JDialog;
import javax.swing.Timer;

public class AuditLogService {

    private static volatile boolean schemaChecked = false;

    public static void logAction(String actionType, String entityType, String entityId, String oldValue, String newValue, String reason) {
        Account currentUser = LoginService.getCurrentUser();
        String accountId = currentUser != null ? currentUser.getAccountId() : null;

        try (Connection con = DatabaseConnection.getConnection()) {
            ensureAuditBranchColumns(con);

            String storeId = resolveCurrentStoreId(con, accountId);
            String storeName = resolveStoreDisplayName(con, storeId);
            String moduleName = resolveModuleName(entityType);
            String logId = "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            String sql = "INSERT INTO AUDIT_LOG ("
                    + "LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID, "
                    + "OLD_VALUE, NEW_VALUE, REASON, IP_ADDRESS, DEVICE_INFO, "
                    + "STORE_ID, STORE_NAME, MODULE_NAME, IS_DELETED, CREATED_AT"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, SYSDATE)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, logId);
                ps.setString(2, accountId);
                ps.setString(3, nvl(actionType, "THAO TÁC"));
                ps.setString(4, nvl(entityType, "HỆ THỐNG"));
                ps.setString(5, nvl(entityId, "N/A"));
                ps.setString(6, oldValue);
                ps.setString(7, newValue);
                ps.setString(8, reason);
                ps.setString(9, "Localhost");
                ps.setString(10, System.getProperty("os.name", "Unknown Device"));
                ps.setString(11, storeId);
                ps.setString(12, storeName);
                ps.setString(13, moduleName);
                ps.executeUpdate();
            }

            autoHideRoleSuccessDialog(actionType, entityType);

        } catch (Exception e) {
            System.err.println("[AuditLog] Lỗi ghi nhật ký hệ thống: " + e.getMessage());
        }
    }

    public static void logActionWithStore(String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason, String storeId) {

        Account currentUser = LoginService.getCurrentUser();
        String accountId = currentUser != null ? currentUser.getAccountId() : null;

        try (Connection con = DatabaseConnection.getConnection()) {
            ensureAuditBranchColumns(con);

            String cleanStoreId = normalizeStoreId(storeId);
            if (cleanStoreId == null) {
                cleanStoreId = resolveCurrentStoreId(con, accountId);
            }

            String storeName = resolveStoreDisplayName(con, cleanStoreId);
            String moduleName = resolveModuleName(entityType);
            String logId = "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            String sql = "INSERT INTO AUDIT_LOG ("
                    + "LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID, "
                    + "OLD_VALUE, NEW_VALUE, REASON, IP_ADDRESS, DEVICE_INFO, "
                    + "STORE_ID, STORE_NAME, MODULE_NAME, IS_DELETED, CREATED_AT"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, SYSDATE)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, logId);
                ps.setString(2, accountId);
                ps.setString(3, nvl(actionType, "THAO TÁC"));
                ps.setString(4, nvl(entityType, "HỆ THỐNG"));
                ps.setString(5, nvl(entityId, "N/A"));
                ps.setString(6, oldValue);
                ps.setString(7, newValue);
                ps.setString(8, reason);
                ps.setString(9, "Localhost");
                ps.setString(10, System.getProperty("os.name", "Unknown Device"));
                ps.setString(11, cleanStoreId);
                ps.setString(12, storeName);
                ps.setString(13, moduleName);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            System.err.println("[AuditLog] Lỗi ghi nhật ký hệ thống: " + e.getMessage());
        }
    }

    public static void logSystemAction(String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason, String storeId) {

        try (Connection con = DatabaseConnection.getConnection()) {
            ensureAuditBranchColumns(con);

            String cleanStoreId = normalizeStoreId(storeId);
            if (cleanStoreId == null) {
                cleanStoreId = "CENTRAL";
            }

            String storeName = resolveStoreDisplayName(con, cleanStoreId);
            String moduleName = resolveModuleName(entityType);
            String logId = "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            String sql = "INSERT INTO AUDIT_LOG ("
                    + "LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID, "
                    + "OLD_VALUE, NEW_VALUE, REASON, IP_ADDRESS, DEVICE_INFO, "
                    + "STORE_ID, STORE_NAME, MODULE_NAME, IS_DELETED, CREATED_AT"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, SYSDATE)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, logId);
                ps.setString(2, "SYSTEM");
                ps.setString(3, nvl(actionType, "HỆ THỐNG"));
                ps.setString(4, nvl(entityType, "HỆ THỐNG"));
                ps.setString(5, nvl(entityId, "N/A"));
                ps.setString(6, oldValue);
                ps.setString(7, newValue);
                ps.setString(8, reason);
                ps.setString(9, "Localhost");
                ps.setString(10, "System Automated Task");
                ps.setString(11, cleanStoreId);
                ps.setString(12, storeName);
                ps.setString(13, moduleName);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            System.err.println("[AuditLog] Lỗi ghi nhật ký hệ thống: " + e.getMessage());
        }
    }

    private static synchronized void ensureAuditBranchColumns(Connection con) {
        if (schemaChecked) {
            return;
        }

        addColumnIfMissing(con, "STORE_ID", "VARCHAR2(30)");
        addColumnIfMissing(con, "STORE_NAME", "NVARCHAR2(150)");
        addColumnIfMissing(con, "MODULE_NAME", "NVARCHAR2(100)");
        schemaChecked = true;
    }

    private static void addColumnIfMissing(Connection con, String columnName, String columnType) {
        String checkSql = "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'AUDIT_LOG' AND COLUMN_NAME = ?";

        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setString(1, columnName.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement st = con.createStatement()) {
                st.executeUpdate("ALTER TABLE AUDIT_LOG ADD (" + columnName + " " + columnType + ")");
            }

        } catch (Exception e) {
            System.err.println("[AuditLog] Bỏ qua thêm cột " + columnName + ": " + e.getMessage());
        }
    }

    private static String resolveCurrentStoreId(Connection con, String accountId) {
        String storeId = null;

        try {
            storeId = normalizeStoreId(SessionManager.getCurrentStoreId());
        } catch (Exception ignored) {
        }

        if (storeId != null) {
            return storeId;
        }

        if (accountId == null || accountId.trim().isEmpty()) {
            return "CENTRAL";
        }

        String sql = "SELECT e.store_id FROM ACCOUNTS a JOIN EMPLOYEES e ON e.employee_id = a.user_id "
                + "WHERE a.account_id = ? FETCH FIRST 1 ROWS ONLY";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    storeId = normalizeStoreId(rs.getString("store_id"));
                }
            }
        } catch (Exception ignored) {
        }

        return storeId == null ? "CENTRAL" : storeId;
    }

    private static String resolveStoreDisplayName(Connection con, String storeId) {
        String cleanStoreId = normalizeStoreId(storeId);

        if (cleanStoreId == null || "CENTRAL".equalsIgnoreCase(cleanStoreId)) {
            return "Trung tâm";
        }

        String nameColumn = firstExistingColumn(con, "STORES",
                new String[]{"STORE_NAME", "NAME", "BRANCH_NAME", "STORE_ADDRESS", "ADDRESS"});

        if (nameColumn == null) {
            return cleanStoreId;
        }

        String sql = "SELECT " + nameColumn + " FROM STORES WHERE store_id = ? FETCH FIRST 1 ROWS ONLY";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cleanStoreId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString(1);
                    if (value != null && !value.trim().isEmpty()) {
                        return cleanStoreId + " - " + value.trim();
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return cleanStoreId;
    }

    private static String firstExistingColumn(Connection con, String tableName, String[] columnNames) {
        String sql = "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

        for (String col : columnNames) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, tableName.toUpperCase());
                ps.setString(2, col.toUpperCase());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return col;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static String resolveModuleName(String entityType) {
        if (entityType == null) {
            return "Hệ thống";
        }

        String e = entityType.toUpperCase();

        if (e.contains("ACCOUNT") || e.contains("ROLE") || e.contains("USER")) {
            return "Tài khoản & phân quyền";
        }
        if (e.contains("EMPLOYEE") || e.contains("SHIFT")) {
            return "Nhân viên & phân ca";
        }
        if (e.contains("PRODUCT") || e.contains("INVENTORY") || e.contains("PURCHASE") || e.contains("SUPPLIER")) {
            return "Kho hàng";
        }
        if (e.contains("ORDER") || e.contains("INVOICE") || e.contains("SALE")) {
            return "Bán hàng";
        }
        if (e.contains("CUSTOMER")) {
            return "Khách hàng";
        }
        if (e.contains("PROMOTION") || e.contains("VOUCHER")) {
            return "Khuyến mãi";
        }
        if (e.contains("STORE")) {
            return "Chi nhánh";
        }

        return "Hệ thống";
    }

    private static String normalizeStoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();

        if ("null".equalsIgnoreCase(text)
                || "Tất cả chi nhánh".equalsIgnoreCase(text)
                || "Chưa xác định".equalsIgnoreCase(text)) {
            return null;
        }

        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }

        return text;
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static void autoHideRoleSuccessDialog(String actionType, String entityType) {
        if (!"CẬP NHẬT".equalsIgnoreCase(String.valueOf(actionType).trim())
                || !"ACCOUNTS".equalsIgnoreCase(String.valueOf(entityType).trim())) {
            return;
        }

        final int[] ticks = {0};
        Timer timer = new Timer(80, null);
        timer.addActionListener(e -> {
            ticks[0]++;

            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog
                        && dialog.isShowing()
                        && "Thành công".equals(dialog.getTitle())) {
                    dialog.setVisible(false);
                }
            }

            if (ticks[0] >= 20) {
                timer.stop();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
    // Thêm vào cuối AuditLogService.java

public static void logActionAsync(
        String actionType,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String reason
) {
    Thread t = new Thread(() -> {
        try {
            logAction(actionType, entityType, entityId, oldValue, newValue, reason);
        } catch (Exception e) {
            System.err.println("[AuditLog] async error: " + e.getMessage());
        }
    }, "audit-log-async");
    t.setDaemon(true);
    t.start();
}

public static void logActionWithStoreAsync(
        String actionType,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String reason,
        String storeId
) {
    Thread t = new Thread(() -> {
        try {
            logActionWithStore(actionType, entityType, entityId, oldValue, newValue, reason, storeId);
        } catch (Exception e) {
            System.err.println("[AuditLog] async store error: " + e.getMessage());
        }
    }, "audit-log-store-async");
    t.setDaemon(true);
    t.start();
}
    
    
}
