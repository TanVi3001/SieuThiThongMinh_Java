package business.sql.prod_inventory;

import common.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryNotificationSql {

    private static InventoryNotificationSql instance;

    private InventoryNotificationSql() {
    }

    public static InventoryNotificationSql getInstance() {
        if (instance == null) {
            instance = new InventoryNotificationSql();
        }
        return instance;
    }

    public static class InventoryNotifDTO {

        public String notificationId;
        public String productId;
        public String productName;
        public String message;
        public int remindCount;
        public Timestamp createdAt;
        public Timestamp updatedAt;
    }

    public void createOrRemindAlert(String productId, String productName, String message, String createdBy) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String checkSql = """
            SELECT notification_id
            FROM INVENTORY_NOTIFICATIONS
            WHERE product_id = ?
              AND status = 'PENDING'
              AND NVL(is_deleted, 0) = 0
            FETCH FIRST 1 ROWS ONLY
        """;

        String updateSql = """
            UPDATE INVENTORY_NOTIFICATIONS
            SET message = ?,
                product_name = ?,
                remind_count = NVL(remind_count, 0) + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE notification_id = ?
        """;

        String insertSql = """
            INSERT INTO INVENTORY_NOTIFICATIONS
            (
                notification_id,
                product_id,
                product_name,
                message,
                target_role,
                status,
                remind_count,
                created_by,
                created_at,
                updated_at,
                is_deleted
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                'WAREHOUSE',
                'PENDING',
                1,
                ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String existingId = null;

                try (PreparedStatement psCheck = con.prepareStatement(checkSql)) {
                    psCheck.setString(1, productId.trim());

                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getString("notification_id");
                        }
                    }
                }

                if (existingId != null) {
                    try (PreparedStatement psUpdate = con.prepareStatement(updateSql)) {
                        psUpdate.setString(1, message);
                        psUpdate.setString(2, productName);
                        psUpdate.setString(3, existingId);
                        psUpdate.executeUpdate();
                    }
                } else {
                    String notificationId = "INV_NOTI_" + System.currentTimeMillis();

                    try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                        psInsert.setString(1, notificationId);
                        psInsert.setString(2, productId.trim());
                        psInsert.setString(3, productName);
                        psInsert.setString(4, message);
                        psInsert.setString(5, createdBy);
                        psInsert.executeUpdate();
                    }
                }

                con.commit();

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<InventoryNotifDTO> getPendingWarehouseAlerts() {
        List<InventoryNotifDTO> list = new ArrayList<>();

        String sql = """
            SELECT notification_id,
                   product_id,
                   product_name,
                   message,
                   remind_count,
                   created_at,
                   updated_at
            FROM INVENTORY_NOTIFICATIONS
            WHERE status = 'PENDING'
              AND target_role = 'WAREHOUSE'
              AND NVL(is_deleted, 0) = 0
            ORDER BY updated_at DESC
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryNotifDTO dto = new InventoryNotifDTO();

                dto.notificationId = rs.getString("notification_id");
                dto.productId = rs.getString("product_id");
                dto.productName = rs.getString("product_name");
                dto.message = rs.getString("message");
                dto.remindCount = rs.getInt("remind_count");
                dto.createdAt = rs.getTimestamp("created_at");
                dto.updatedAt = rs.getTimestamp("updated_at");

                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void resolveByProductIdWithConn(Connection con, String productId) throws SQLException {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String sql = """
            UPDATE INVENTORY_NOTIFICATIONS
            SET status = 'RESOLVED',
                resolved_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE product_id = ?
              AND status = 'PENDING'
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());
            ps.executeUpdate();
        }
    }
}
