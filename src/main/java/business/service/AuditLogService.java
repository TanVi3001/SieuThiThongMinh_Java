package business.service;

import common.db.DatabaseConnection;
import model.account.Account;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class AuditLogService {

    public static void logAction(String actionType, String entityType, String entityId, String oldValue, String newValue, String reason) {
        // Lấy thông tin người đang thao tác
        Account currentUser = LoginService.getCurrentUser();
        String accountId = (currentUser != null) ? currentUser.getAccountId() : null;

        // Tạo mã Log ngẫu nhiên (Ví dụ: LOG_A1B2C3D4)
        String logId = "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        String sql = "INSERT INTO AUDIT_LOG (LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID, OLD_VALUE, NEW_VALUE, REASON) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, logId);
            ps.setString(2, accountId);
            ps.setString(3, actionType);     // THÊM MỚI, CẬP NHẬT, XÓA...
            ps.setString(4, entityType);     // ACCOUNTS, ROLES, PRODUCTS...
            ps.setString(5, entityId);       // Mã đối tượng bị tác động
            ps.setString(6, oldValue);
            ps.setString(7, newValue);
            ps.setString(8, reason);

            ps.executeUpdate();
            System.out.println("📝 [AuditLog] Đã ghi nhận thành công: " + actionType + " trên " + entityType);

        } catch (Exception e) {
            System.err.println("❌ [AuditLog] Lỗi ghi nhật ký hệ thống: " + e.getMessage());
        }
    }
}