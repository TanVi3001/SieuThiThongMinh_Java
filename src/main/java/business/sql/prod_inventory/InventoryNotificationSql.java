package business.sql.prod_inventory;

import business.service.AuthorizationService;
import business.service.SessionManager;
import common.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL xử lý thông báo tồn kho.
 *
 * Logic mới: - Thông báo kho phải được cô lập theo store_id nếu bảng có cột
 * STORE_ID. - User chi nhánh chỉ đọc/ghi thông báo của chi nhánh hiện tại. -
 * Admin có thể đọc toàn bộ, nhưng khi tạo thông báo vẫn cố gắng gắn store_id
 * theo session hoặc suy luận từ INVENTORY. - Giữ các method cũ để không vỡ code
 * hiện tại.
 */
public class InventoryNotificationSql {

    private static final int DEFAULT_SAFE_QUANTITY = 20;

    private static InventoryNotificationSql instance;

    private InventoryNotificationSql() {
    }

    public static InventoryNotificationSql getInstance() {
        if (instance == null) {
            instance = new InventoryNotificationSql();
        }
        return instance;
    }

    public static class NotifyResult {

        public boolean success;
        public String message;
        public int currentCount;
        public int maxCount;
        public int waitMinutes;

        public NotifyResult(boolean success, String message, int currentCount, int maxCount, int waitMinutes) {
            this.success = success;
            this.message = message;
            this.currentCount = currentCount;
            this.maxCount = maxCount;
            this.waitMinutes = waitMinutes;
        }
    }

    public static class InventoryNotificationDTO {

        public String notificationId;
        public String productId;
        public String productName;
        public int currentQuantity;
        public String title;
        public String message;
        public String notifyType;
        public String status;
        public int clickCount;
        public String createdBy;
        public String storeId;
        public Timestamp createdAt;
        public Timestamp updatedAt;
    }

    /**
     * DTO cũ để không vỡ code ở NotificationBell hoặc các file cũ.
     */
    public static class InventoryNotifDTO {

        public String notificationId;
        public String productId;
        public String productName;
        public String message;
        public int remindCount;
        public int currentQuantity;
        public String storeId;
        public Timestamp createdAt;
        public Timestamp updatedAt;
    }

    private boolean isManagerOrAdmin() {
        try {
            return AuthorizationService.isStoreManager() || AuthorizationService.isAdmin();
        } catch (Exception e) {
            return false;
        }
    }

    private int getCooldownMinutesByCurrentRole() {
        if (isManagerOrAdmin()) {
            return 0;
        }
        return 10;
    }

    private boolean tableExists(Connection con, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_tables WHERE table_name = UPPER(?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean columnExists(Connection con, String tableName, String columnName) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM user_tab_columns
            WHERE table_name = UPPER(?)
              AND column_name = UPPER(?)
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private String countColumn(Connection con) throws SQLException {
        if (columnExists(con, "INVENTORY_NOTIFICATIONS", "REMIND_COUNT")) {
            return "remind_count";
        }

        if (columnExists(con, "INVENTORY_NOTIFICATIONS", "CLICK_COUNT")) {
            return "click_count";
        }

        return null;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    /**
     * Đếm pending theo sản phẩm và tự scope theo store hiện tại nếu user không
     * phải Admin.
     */
    public int countPendingByProduct(String productId) {
        return countPendingByProductAndStore(productId, currentStoreIdOrNull());
    }

    public int countPendingByProductAndStore(String productId, String storeId) {
        if (productId == null || productId.trim().isEmpty()) {
            return 0;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return 0;
            }

            boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
            String cleanStoreId = normalizeStoreId(storeId);
            if (!SessionManager.isAdmin()) {
                cleanStoreId = currentStoreIdOrNull();
            }

            String countCol = countColumn(con);
            String countExpr = countCol == null ? "1" : "NVL(" + countCol + ", 1)";

            String sql = """
                SELECT NVL(SUM(%s), 0)
                FROM INVENTORY_NOTIFICATIONS
                WHERE REGEXP_REPLACE(UPPER(TRIM(product_id)), '^SP0*', 'SP')
                      = REGEXP_REPLACE(UPPER(TRIM(?)), '^SP0*', 'SP')
                  AND target_role = 'WAREHOUSE'
                  AND status = 'PENDING'
                  AND NVL(is_deleted, 0) = 0
            """.formatted(countExpr)
                    + (hasStoreId && cleanStoreId != null ? " AND (store_id = ? OR store_id IS NULL) " : "");

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int idx = 1;
                ps.setString(idx++, productId.trim());

                if (hasStoreId && cleanStoreId != null) {
                    ps.setString(idx++, cleanStoreId);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean createOrIncreaseLowStockNotification(
            String productId,
            String productName,
            int currentQuantity,
            String createdBy
    ) {
        NotifyResult result = createOrIncreaseLowStockNotificationWithCooldown(
                productId,
                productName,
                currentQuantity,
                createdBy
        );

        return result.success;
    }

    public NotifyResult createOrIncreaseLowStockNotificationWithCooldown(
            String productId,
            String productName,
            int currentQuantity,
            String createdBy
    ) {
        if (productId == null || productId.trim().isEmpty()) {
            return new NotifyResult(false, "Mã sản phẩm không hợp lệ.", 0, 3, 0);
        }

        productId = productId.trim();
        productName = safe(productName, productId);
        createdBy = safe(createdBy, "UNKNOWN");

        boolean isManager = isManagerOrAdmin();
        int cooldownMinutes = getCooldownMinutesByCurrentRole();

        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return new NotifyResult(
                        false,
                        "Thiếu bảng INVENTORY_NOTIFICATIONS. Vui lòng chạy patch SQL tạo bảng thông báo tồn kho.",
                        0,
                        3,
                        cooldownMinutes
                );
            }

            boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
            boolean hasProductName = columnExists(con, "INVENTORY_NOTIFICATIONS", "PRODUCT_NAME");
            boolean hasTitle = columnExists(con, "INVENTORY_NOTIFICATIONS", "TITLE");
            boolean hasNotifyType = columnExists(con, "INVENTORY_NOTIFICATIONS", "NOTIFY_TYPE");
            boolean hasCreatedBy = columnExists(con, "INVENTORY_NOTIFICATIONS", "CREATED_BY");
            String countCol = countColumn(con);

            String storeId = currentStoreIdOrNull();
            if (storeId == null) {
                storeId = inferStoreIdByProduct(con, productId);
            }

            String countExpr = countCol == null ? "1" : "NVL(" + countCol + ", 1)";

            String checkSql = """
                SELECT
                    notification_id,
                    %s AS current_count,
                    updated_at,
                    FLOOR((CAST(SYSTIMESTAMP AS DATE) - CAST(updated_at AS DATE)) * 24 * 60) AS minutes_passed
                FROM INVENTORY_NOTIFICATIONS
                WHERE REGEXP_REPLACE(UPPER(TRIM(product_id)), '^SP0*', 'SP')
                      = REGEXP_REPLACE(UPPER(TRIM(?)), '^SP0*', 'SP')
                  AND target_role = 'WAREHOUSE'
                  AND status = 'PENDING'
                  AND NVL(is_deleted, 0) = 0
            """.formatted(countExpr)
                    + (hasStoreId && storeId != null ? " AND (store_id = ? OR store_id IS NULL) " : "")
                    + """
                ORDER BY updated_at DESC, created_at DESC
                FETCH FIRST 1 ROWS ONLY
            """;

            con.setAutoCommit(false);

            try {
                String existingId = null;
                int currentCount = 0;
                int minutesPassed = 999999;

                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    int idx = 1;
                    ps.setString(idx++, productId);

                    if (hasStoreId && storeId != null) {
                        ps.setString(idx++, storeId);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getString("notification_id");
                            currentCount = rs.getInt("current_count");
                            minutesPassed = rs.getInt("minutes_passed");
                        }
                    }
                }

                if (existingId != null) {
                    if (!isManager && currentCount >= 3) {
                        con.rollback();
                        return new NotifyResult(
                                false,
                                "Sản phẩm này đã được báo 3 lần rồi. Vui lòng chờ nhân viên kho xử lý.",
                                currentCount,
                                3,
                                cooldownMinutes
                        );
                    }

                    if (!isManager && minutesPassed < cooldownMinutes) {
                        int remain = Math.max(cooldownMinutes - minutesPassed, 1);
                        con.rollback();
                        return new NotifyResult(
                                false,
                                "Bạn vừa báo sản phẩm này gần đây. Vui lòng chờ thêm khoảng "
                                + remain + " phút trước khi báo lại.",
                                currentCount,
                                3,
                                cooldownMinutes
                        );
                    }

                    int nextCount = currentCount + 1;
                    String title = currentQuantity <= 0 ? "Sản phẩm đã hết hàng" : "Sản phẩm sắp hết hàng";
                    String msg = buildMessage(productName, currentQuantity, nextCount, isManager);

                    StringBuilder updateSql = new StringBuilder();
                    List<Object> params = new ArrayList<>();

                    updateSql.append("UPDATE INVENTORY_NOTIFICATIONS SET ");
                    updateSql.append("message = ?, updated_at = CURRENT_TIMESTAMP");
                    params.add(msg);

                    if (hasProductName) {
                        updateSql.append(", product_name = ?");
                        params.add(productName);
                    }

                    if (hasTitle) {
                        updateSql.append(", title = ?");
                        params.add(title);
                    }

                    if (hasNotifyType) {
                        updateSql.append(", notify_type = 'LOW_STOCK'");
                    }

                    if (hasCreatedBy) {
                        updateSql.append(", created_by = ?");
                        params.add(createdBy);
                    }

                    if (hasStoreId && storeId != null) {
                        updateSql.append(", store_id = ?");
                        params.add(storeId);
                    }

                    if (countCol != null) {
                        updateSql.append(", ").append(countCol)
                                .append(" = NVL(").append(countCol).append(", 0) + 1");
                    }

                    updateSql.append(" WHERE notification_id = ?");
                    params.add(existingId);

                    try (PreparedStatement ps = con.prepareStatement(updateSql.toString())) {
                        bindParams(ps, params);
                        ps.executeUpdate();
                    }

                    con.commit();

                    String sender = getSenderLabel();
                    String successMsg = isManager
                            ? sender + " đã nhắc kho lần " + nextCount + "."
                            : sender + " đã gửi thông báo cho kho. Số lần báo: " + nextCount + "/3.";

                    return new NotifyResult(true, successMsg, nextCount, 3, cooldownMinutes);
                }

                int firstCount = 1;
                String notificationId = "INV_NOTI_" + System.currentTimeMillis();
                String title = currentQuantity <= 0 ? "Sản phẩm đã hết hàng" : "Sản phẩm sắp hết hàng";
                String msg = buildMessage(productName, currentQuantity, firstCount, isManager);

                StringBuilder insertSql = new StringBuilder();
                List<String> columns = new ArrayList<>();
                List<String> placeholders = new ArrayList<>();
                List<Object> params = new ArrayList<>();

                addParam(columns, placeholders, params, "notification_id", notificationId);
                addParam(columns, placeholders, params, "product_id", productId);

                if (hasStoreId && storeId != null) {
                    addParam(columns, placeholders, params, "store_id", storeId);
                }

                if (hasProductName) {
                    addParam(columns, placeholders, params, "product_name", productName);
                }

                if (hasTitle) {
                    addParam(columns, placeholders, params, "title", title);
                }

                addParam(columns, placeholders, params, "message", msg);

                if (hasNotifyType) {
                    addParam(columns, placeholders, params, "notify_type", "LOW_STOCK");
                }

                addParam(columns, placeholders, params, "target_role", "WAREHOUSE");
                addParam(columns, placeholders, params, "status", "PENDING");

                if (countCol != null) {
                    addParam(columns, placeholders, params, countCol, firstCount);
                }

                if (hasCreatedBy) {
                    addParam(columns, placeholders, params, "created_by", createdBy);
                }

                columns.add("created_at");
                placeholders.add("CURRENT_TIMESTAMP");

                columns.add("updated_at");
                placeholders.add("CURRENT_TIMESTAMP");

                addParam(columns, placeholders, params, "is_deleted", 0);

                insertSql.append("INSERT INTO INVENTORY_NOTIFICATIONS (");
                insertSql.append(String.join(", ", columns));
                insertSql.append(") VALUES (");
                insertSql.append(String.join(", ", placeholders));
                insertSql.append(")");

                try (PreparedStatement ps = con.prepareStatement(insertSql.toString())) {
                    bindParams(ps, params);
                    ps.executeUpdate();
                }

                con.commit();

                String sender = getSenderLabel();
                String successMsg = isManager
                        ? sender + " đã gửi nhắc kho lần 1."
                        : sender + " đã gửi thông báo cho kho. Số lần báo: 1/3.";

                return new NotifyResult(true, successMsg, firstCount, 3, cooldownMinutes);

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new NotifyResult(
                    false,
                    "Lỗi khi gửi thông báo: " + e.getMessage(),
                    0,
                    3,
                    cooldownMinutes
            );
        }
    }

    private void addParam(List<String> columns, List<String> placeholders, List<Object> params, String column, Object value) {
        columns.add(column);
        placeholders.add("?");
        params.add(value);
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        int idx = 1;

        for (Object value : params) {
            if (value instanceof Integer n) {
                ps.setInt(idx++, n);
            } else if (value instanceof Long n) {
                ps.setLong(idx++, n);
            } else if (value instanceof Double n) {
                ps.setDouble(idx++, n);
            } else if (value instanceof Timestamp t) {
                ps.setTimestamp(idx++, t);
            } else {
                ps.setString(idx++, value == null ? null : String.valueOf(value));
            }
        }
    }

    private String getSenderLabel() {
        if (AuthorizationService.isAdmin()) {
            return "Admin";
        }

        if (AuthorizationService.isStoreManager()) {
            return "Quản lý";
        }

        if (AuthorizationService.isCashier()) {
            return "Nhân viên bán hàng";
        }

        if (AuthorizationService.isWarehouseStaff()) {
            return "Nhân viên kho";
        }

        return "Nhân viên";
    }

    private String buildMessage(String productName, int currentQuantity, int count, boolean isManager) {
        String sender = getSenderLabel();

        String statusText;

        if (currentQuantity <= 0) {
            statusText = "Sản phẩm " + productName + " đã hết hàng. Cần nhập khẩn!";
        } else {
            statusText = "Sản phẩm " + productName
                    + " đang còn " + currentQuantity
                    + " sản phẩm. Nên lên kế hoạch nhập thêm.";
        }

        if (isManager) {
            return sender + " đã nhắc kho lần " + count + ". " + statusText;
        }

        return sender + " đã báo kho " + count + "/3 lần. " + statusText;
    }

    /**
     * Admin: xem tất cả nếu không truyền store. User chi nhánh: tự ép theo
     * store session.
     */
    public List<InventoryNotificationDTO> getPendingWarehouseNotifications() {
        return getPendingWarehouseNotificationsByStore(currentStoreIdOrNull());
    }

    public List<InventoryNotificationDTO> getPendingWarehouseNotificationsByStore(String storeId) {
        List<InventoryNotificationDTO> list = new ArrayList<>();

        String cleanStoreId = normalizeStoreId(storeId);
        if (!SessionManager.isAdmin()) {
            cleanStoreId = currentStoreIdOrNull();
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return list;
            }

            boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
            boolean hasProductName = columnExists(con, "INVENTORY_NOTIFICATIONS", "PRODUCT_NAME");
            boolean hasTitle = columnExists(con, "INVENTORY_NOTIFICATIONS", "TITLE");
            boolean hasNotifyType = columnExists(con, "INVENTORY_NOTIFICATIONS", "NOTIFY_TYPE");
            boolean hasCreatedBy = columnExists(con, "INVENTORY_NOTIFICATIONS", "CREATED_BY");
            String countCol = countColumn(con);

            String productNameExpr = hasProductName
                    ? "NVL(n.product_name, p.product_name)"
                    : "p.product_name";

            String titleExpr = hasTitle
                    ? "NVL(n.title, 'Cảnh báo tồn kho')"
                    : "'Cảnh báo tồn kho'";

            String notifyTypeExpr = hasNotifyType
                    ? "NVL(n.notify_type, 'LOW_STOCK')"
                    : "'LOW_STOCK'";

            String countExpr = countCol == null
                    ? "1"
                    : "NVL(n." + countCol + ", 1)";

            String createdByExpr = hasCreatedBy
                    ? "n.created_by"
                    : "NULL";

            String storeSelectExpr = hasStoreId ? "n.store_id" : "NULL";

            String invJoin;
            String storeFilter = "";
            boolean bindStoreInJoin = false;
            boolean bindStoreInWhere = false;

            if (hasStoreId && cleanStoreId != null) {
                invJoin = """
                    LEFT JOIN (
                        SELECT
                            product_id,
                            store_id,
                            SUM(NVL(quantity, 0)) AS current_quantity,
                            MAX(last_updated) AS last_updated
                        FROM INVENTORY
                        WHERE NVL(is_deleted, 0) = 0
                        GROUP BY product_id, store_id
                    ) inv
                        ON REGEXP_REPLACE(UPPER(TRIM(inv.product_id)), '^SP0*', 'SP')
                           = REGEXP_REPLACE(UPPER(TRIM(n.product_id)), '^SP0*', 'SP')
                       AND inv.store_id = ?
                """;
                storeFilter = " AND (n.store_id = ? OR n.store_id IS NULL) ";
                bindStoreInJoin = true;
                bindStoreInWhere = true;
            } else {
                invJoin = """
                    LEFT JOIN (
                        SELECT
                            product_id,
                            SUM(NVL(quantity, 0)) AS current_quantity,
                            MAX(last_updated) AS last_updated
                        FROM INVENTORY
                        WHERE NVL(is_deleted, 0) = 0
                        GROUP BY product_id
                    ) inv
                        ON REGEXP_REPLACE(UPPER(TRIM(inv.product_id)), '^SP0*', 'SP')
                           = REGEXP_REPLACE(UPPER(TRIM(n.product_id)), '^SP0*', 'SP')
                """;
            }

            String sql = """
                SELECT
                    n.notification_id,
                    n.product_id,
                    %s AS store_id,
                    %s AS product_name,
                    NVL(inv.current_quantity, 0) AS current_quantity,
                    %s AS title,
                    n.message,
                    %s AS notify_type,
                    n.status,
                    %s AS click_count,
                    %s AS created_by,
                    n.created_at,
                    n.updated_at
                FROM INVENTORY_NOTIFICATIONS n
                LEFT JOIN PRODUCTS p
                    ON REGEXP_REPLACE(UPPER(TRIM(p.product_id)), '^SP0*', 'SP')
                       = REGEXP_REPLACE(UPPER(TRIM(n.product_id)), '^SP0*', 'SP')
                %s
                WHERE n.target_role = 'WAREHOUSE'
                  AND n.status = 'PENDING'
                  AND NVL(n.is_deleted, 0) = 0
                  AND (
                      NVL(inv.current_quantity, 0) <= ?
                      OR inv.last_updated IS NULL
                      OR CAST(inv.last_updated AS TIMESTAMP) <= n.updated_at
                  )
            """.formatted(
                    storeSelectExpr,
                    productNameExpr,
                    titleExpr,
                    notifyTypeExpr,
                    countExpr,
                    createdByExpr,
                    invJoin
            ) + storeFilter
                    + """
                ORDER BY n.updated_at DESC, n.created_at DESC
            """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int idx = 1;

                if (bindStoreInJoin) {
                    ps.setString(idx++, cleanStoreId);
                }

                ps.setInt(idx++, DEFAULT_SAFE_QUANTITY);

                if (bindStoreInWhere) {
                    ps.setString(idx++, cleanStoreId);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        InventoryNotificationDTO x = new InventoryNotificationDTO();
                        x.notificationId = rs.getString("notification_id");
                        x.productId = rs.getString("product_id");
                        x.storeId = rs.getString("store_id");
                        x.productName = rs.getString("product_name");
                        x.currentQuantity = rs.getInt("current_quantity");
                        x.title = rs.getString("title");
                        x.message = rs.getString("message");
                        x.notifyType = rs.getString("notify_type");
                        x.status = rs.getString("status");
                        x.clickCount = rs.getInt("click_count");
                        x.createdBy = rs.getString("created_by");
                        x.createdAt = rs.getTimestamp("created_at");
                        x.updatedAt = rs.getTimestamp("updated_at");
                        list.add(x);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<InventoryNotifDTO> getPendingWarehouseAlerts() {
        return getPendingWarehouseAlertsByStore(currentStoreIdOrNull());
    }

    public List<InventoryNotifDTO> getPendingWarehouseAlertsByStore(String storeId) {
        List<InventoryNotifDTO> oldList = new ArrayList<>();

        for (InventoryNotificationDTO n : getPendingWarehouseNotificationsByStore(storeId)) {
            InventoryNotifDTO x = new InventoryNotifDTO();
            x.notificationId = n.notificationId;
            x.productId = n.productId;
            x.productName = n.productName;
            x.message = n.message;
            x.remindCount = n.clickCount;
            x.currentQuantity = n.currentQuantity;
            x.storeId = n.storeId;
            x.createdAt = n.createdAt;
            x.updatedAt = n.updatedAt;
            oldList.add(x);
        }

        return oldList;
    }

    public void createOrRemindAlert(String productId, String productName, String message, String createdBy) {
        createOrIncreaseLowStockNotificationWithCooldown(productId, productName, 0, createdBy);
    }

    public boolean resolveNotification(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            return false;
        }

        String sql = """
            UPDATE INVENTORY_NOTIFICATIONS
            SET status = 'RESOLVED',
                resolved_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE notification_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, notificationId.trim());
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean resolveByProductId(String productId) {
        return resolveByProductIdAndStore(productId, currentStoreIdOrNull());
    }

    public boolean resolveByProductIdAndStore(String productId, String storeId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return false;
            }

            boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
            String cleanStoreId = normalizeStoreId(storeId);
            if (!SessionManager.isAdmin()) {
                cleanStoreId = currentStoreIdOrNull();
            }

            String sql = """
                UPDATE INVENTORY_NOTIFICATIONS
                SET status = 'RESOLVED',
                    resolved_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE REGEXP_REPLACE(UPPER(TRIM(product_id)), '^SP0*', 'SP')
                      = REGEXP_REPLACE(UPPER(TRIM(?)), '^SP0*', 'SP')
                  AND status = 'PENDING'
                  AND NVL(is_deleted, 0) = 0
            """ + (hasStoreId && cleanStoreId != null ? " AND (store_id = ? OR store_id IS NULL) " : "");

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int idx = 1;
                ps.setString(idx++, productId.trim());

                if (hasStoreId && cleanStoreId != null) {
                    ps.setString(idx++, cleanStoreId);
                }

                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void resolveByProductIdWithConn(Connection con, String productId) throws SQLException {
        resolveByProductIdWithConn(con, productId, currentStoreIdOrNull());
    }

    public void resolveByProductIdWithConn(Connection con, String productId, String storeId) throws SQLException {
        if (con == null || productId == null || productId.trim().isEmpty()) {
            return;
        }

        boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
        String cleanStoreId = normalizeStoreId(storeId);
        if (!SessionManager.isAdmin()) {
            cleanStoreId = currentStoreIdOrNull();
        }

        String sql = """
            UPDATE INVENTORY_NOTIFICATIONS
            SET status = 'RESOLVED',
                resolved_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE REGEXP_REPLACE(UPPER(TRIM(product_id)), '^SP0*', 'SP')
                  = REGEXP_REPLACE(UPPER(TRIM(?)), '^SP0*', 'SP')
              AND status = 'PENDING'
              AND NVL(is_deleted, 0) = 0
        """ + (hasStoreId && cleanStoreId != null ? " AND (store_id = ? OR store_id IS NULL) " : "");

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, productId.trim());

            if (hasStoreId && cleanStoreId != null) {
                ps.setString(idx++, cleanStoreId);
            }

            ps.executeUpdate();
        }
    }

    public int resolveRecoveredPendingNotificationsByStore(String storeId, int safeQuantity) {
        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return 0;
            }

            boolean hasStoreId = columnExists(con, "INVENTORY_NOTIFICATIONS", "STORE_ID");
            String cleanStoreId = normalizeStoreId(storeId);
            if (!SessionManager.isAdmin()) {
                cleanStoreId = currentStoreIdOrNull();
            }

            if (hasStoreId && cleanStoreId != null) {
                String sql = """
                    UPDATE INVENTORY_NOTIFICATIONS n
                    SET n.status = 'RESOLVED',
                        n.resolved_at = CURRENT_TIMESTAMP,
                        n.updated_at = CURRENT_TIMESTAMP
                    WHERE n.target_role = 'WAREHOUSE'
                      AND n.status = 'PENDING'
                      AND NVL(n.is_deleted, 0) = 0
                      AND (n.store_id = ? OR n.store_id IS NULL)
                      AND EXISTS (
                          SELECT 1
                          FROM (
                              SELECT product_id,
                                     store_id,
                                     SUM(NVL(quantity, 0)) AS total_quantity,
                                     MAX(last_updated) AS last_updated
                              FROM INVENTORY
                              WHERE NVL(is_deleted, 0) = 0
                              GROUP BY product_id, store_id
                          ) inv
                          WHERE REGEXP_REPLACE(UPPER(TRIM(inv.product_id)), '^SP0*', 'SP')
                                = REGEXP_REPLACE(UPPER(TRIM(n.product_id)), '^SP0*', 'SP')
                            AND inv.store_id = ?
                            AND inv.total_quantity > ?
                        AND inv.last_updated IS NOT NULL
                        AND CAST(inv.last_updated AS TIMESTAMP) > n.updated_at
                            AND inv.last_updated IS NOT NULL
                            AND CAST(inv.last_updated AS TIMESTAMP) > n.updated_at
                      )
                """;

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, cleanStoreId);
                    ps.setString(2, cleanStoreId);
                    ps.setInt(3, safeQuantity);
                    return ps.executeUpdate();
                }
            }

            return resolveRecoveredPendingNotifications(safeQuantity);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int resolveRecoveredPendingNotifications(int safeQuantity) {
        try (Connection con = DatabaseConnection.getConnection()) {
            if (!tableExists(con, "INVENTORY_NOTIFICATIONS")) {
                return 0;
            }

            String sql = """
                UPDATE INVENTORY_NOTIFICATIONS n
                SET n.status = 'RESOLVED',
                    n.resolved_at = CURRENT_TIMESTAMP,
                    n.updated_at = CURRENT_TIMESTAMP
                WHERE n.target_role = 'WAREHOUSE'
                  AND n.status = 'PENDING'
                  AND NVL(n.is_deleted, 0) = 0
                  AND EXISTS (
                      SELECT 1
                      FROM (
                          SELECT product_id,
                                 SUM(NVL(quantity, 0)) AS total_quantity,
                                 MAX(last_updated) AS last_updated
                          FROM INVENTORY
                          WHERE NVL(is_deleted, 0) = 0
                          GROUP BY product_id
                      ) inv
                      WHERE REGEXP_REPLACE(UPPER(TRIM(inv.product_id)), '^SP0*', 'SP')
                            = REGEXP_REPLACE(UPPER(TRIM(n.product_id)), '^SP0*', 'SP')
                        AND inv.total_quantity > ?
                  )
            """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, safeQuantity);
                return ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String currentStoreIdOrNull() {
        try {
            if (SessionManager.isAdmin()) {
                return null;
            }

            String storeId = SessionManager.getCurrentStoreId();

            if (storeId == null || storeId.trim().isEmpty()) {
                return null;
            }

            return storeId.trim();

        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeStoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();

        if ("Tất cả chi nhánh".equalsIgnoreCase(text)
                || "Chưa xác định".equalsIgnoreCase(text)) {
            return null;
        }

        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }

        return text;
    }

    private String inferStoreIdByProduct(Connection con, String productId) {
        if (con == null || productId == null || productId.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT store_id
            FROM INVENTORY
            WHERE REGEXP_REPLACE(UPPER(TRIM(product_id)), '^SP0*', 'SP')
                  = REGEXP_REPLACE(UPPER(TRIM(?)), '^SP0*', 'SP')
              AND NVL(is_deleted, 0) = 0
            ORDER BY store_id
            FETCH FIRST 1 ROWS ONLY
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return normalizeStoreId(rs.getString("store_id"));
                }
            }
        } catch (Exception e) {
            // Không chặn luồng tạo thông báo chỉ vì không suy luận được store.
        }

        return null;
    }
}
