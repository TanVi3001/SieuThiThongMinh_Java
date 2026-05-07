package business.service;

import model.account.Account;
import common.db.DatabaseConnection;
import common.utils.NetworkUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditLogService {

    /**
     * Ghi lại nhật ký hệ thống.
     * @param actionType Loại hành động (THÊM MỚI, CẬP NHẬT, XÓA, ĐĂNG NHẬP...)
     * @param entityType Đối tượng bị tác động (TÀI KHOẢN, NHÂN VIÊN, SẢN PHẨM...)
     * @param entityId ID của đối tượng bị tác động
     * @param oldValue Dữ liệu cũ (nếu có)
     * @param newValue Dữ liệu mới (nếu có)
     * @param reason Lý do thao tác (nếu có)
     */
    public static void logAction(String actionType, String entityType, String entityId, 
                                 String oldValue, String newValue, String reason) {
        
        new Thread(() -> {
            // SỬ DỤNG BẢNG AUDIT_LOG THEO CHUẨN TIẾNG ANH
            String sql = "INSERT INTO AUDIT_LOG (LOG_ID, ACCOUNT_ID, ACTION_TYPE, ENTITY_TYPE, "
                       + "ENTITY_ID, OLD_VALUE, NEW_VALUE, REASON, IP_ADDRESS, DEVICE_INFO) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                       
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                // Lấy người dùng đang đăng nhập
                Account currentUser = LoginService.getCurrentUser();
                String accountId = (currentUser != null && currentUser.getAccountId() != null) 
                                    ? currentUser.getAccountId() : "SYSTEM";
                
                // Lấy IP nội bộ
                String ipAddress = "127.0.0.1"; 
                try {
                    ipAddress = NetworkUtils.getLocalIPv4Address();
                } catch(Exception ignored) {}

                String logId = "LOG_" + System.currentTimeMillis();
                String deviceInfo = System.getProperty("os.name") + " - Java GUI";

                ps.setString(1, logId);
                ps.setString(2, accountId);
                ps.setString(3, actionType);
                ps.setString(4, entityType);
                ps.setString(5, entityId);
                ps.setString(6, oldValue);
                ps.setString(7, newValue);
                ps.setString(8, reason);
                ps.setString(9, ipAddress);
                ps.setString(10, deviceInfo);

                ps.executeUpdate();

            } catch (Exception e) {
                System.err.println("Lỗi ghi Nhật ký hệ thống (Audit Log): " + e.getMessage());
            }
        }).start();
    }
}