package business.sql.rbac;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.account.AuditLog;

public class AuditLogSql implements SqlInterface<AuditLog> {

    private static AuditLogSql instance;

    public static AuditLogSql getInstance() {
        if (instance == null) {
            instance = new AuditLogSql();
        }
        return instance;
    }

    // =========================================================================
    // HÀM DÙNG CHO TRANSACTION (Giải quyết lỗi đỏ ở AccountActivationService)
    // =========================================================================
    /**
     * Ghi log rút gọn trong một Transaction đang mở. Thường dùng cho các tác vụ
     * Tự động hoặc Hệ thống.
     */
    public void log(Connection conn, String accountId, String actionType, String reason) throws SQLException {
        AuditLog audit = new AuditLog();
        audit.setAccountId(accountId);
        audit.setActionType(actionType);
        audit.setReason(reason);
        audit.setEntityType("ACCOUNT");
        // ---> BỔ SUNG DÒNG NÀY ĐỂ TRÁNH LỖI ORA-01400 <---
        audit.setEntityId(accountId);
        audit.setIpAddress("localhost");

        insertWithConn(conn, audit);
    }

    /**
     * Hàm insert lõi, dùng chung cho tất cả các trường hợp ghi log.
     */
    public int insertWithConn(Connection con, AuditLog t) throws SQLException {
        String sql = "INSERT INTO AUDIT_LOG (log_id, account_id, action_type, entity_type, entity_id, "
                + "old_value, new_value, reason, ip_address, device_info, is_deleted, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, SYSDATE)";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            // Sinh ID log: LOG + thời gian + số ngẫu nhiên để tránh trùng
            String logId = t.getLogId();
            if (logId == null || logId.trim().isEmpty()) {
                logId = "LOG_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
            }

            pst.setString(1, logId);
            pst.setString(2, t.getAccountId());
            pst.setString(3, t.getActionType());
            pst.setString(4, t.getEntityType());
            pst.setString(5, t.getEntityId());
            pst.setString(6, t.getOldValue());
            pst.setString(7, t.getNewValue());
            pst.setString(8, t.getReason());
            pst.setString(9, t.getIpAddress() != null ? t.getIpAddress() : "localhost");
            pst.setString(10, t.getDeviceInfo());

            return pst.executeUpdate();
        }
    }

    // =========================================================================
    // CÁC HÀM TIÊU CHUẨN (SqlInterface)
    // =========================================================================
    @Override
    public int insert(AuditLog t) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertWithConn(con, t);
        } catch (SQLException e) {
            System.err.println("Lỗi AuditLogSql.insert: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int update(AuditLog t) {
        // Audit Log là dữ liệu lịch sử, hạn chế update trừ trường hợp cần note thêm lý do
        String sql = "UPDATE AUDIT_LOG SET reason = ?, is_deleted = ? WHERE log_id = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getReason());
            pst.setInt(2, t.getIsDeleted());
            pst.setString(3, t.getLogId());
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        // Sử dụng Soft Delete (is_deleted = 1) để giữ lại dấu vết bảo mật
        String sql = "UPDATE AUDIT_LOG SET is_deleted = 1 WHERE log_id = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public AuditLog selectById(String id) {
        String sql = "SELECT * FROM AUDIT_LOG WHERE log_id = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ArrayList<AuditLog> selectAll() {
        ArrayList<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM AUDIT_LOG WHERE is_deleted = 0 ORDER BY created_at DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<AuditLog> selectByCondition(String condition) {
        ArrayList<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM AUDIT_LOG WHERE is_deleted = 0 "
                + (condition == null ? "" : "AND " + condition) + " ORDER BY created_at DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getString("log_id"));
        log.setAccountId(rs.getString("account_id"));
        log.setActionType(rs.getString("action_type"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getString("entity_id"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setReason(rs.getString("reason"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setDeviceInfo(rs.getString("device_info"));
        try {
            log.setCreatedAt(rs.getTimestamp("created_at"));
        } catch (Exception ignored) {
        }
        log.setIsDeleted(rs.getInt("is_deleted"));
        return log;
    }

    // =========================================================================
    // HÀM TIỆN ÍCH GHI LOG NHANH (Dùng cho các Service không có Transaction)
    // =========================================================================
    public void log(String accountId, String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason,
            String ipAddress, String deviceInfo) {

        AuditLog audit = new AuditLog();
        audit.setAccountId(accountId);
        audit.setActionType(actionType);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setReason(reason);
        audit.setIpAddress(ipAddress);
        audit.setDeviceInfo(deviceInfo);

        this.insert(audit);
    }
}
