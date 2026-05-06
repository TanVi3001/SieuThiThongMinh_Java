package business.service;

import business.sql.rbac.AuditLogSql;
import common.realtime.RealtimeClient; // Thêm import
import common.events.EventBus; // Thêm import
import common.events.AppEventType; // Thêm import
import common.events.AppDataChangedEvent; // Thêm import

public class AccountService {

    /**
     * Ghi audit log cho hành động đổi role user. Gọi hàm này sau khi update
     * role thành công trong AccountSql.
     *
     * @param targetAccountId
     * @param oldRole
     * @param newRole
     * @param reason
     */
    public static void logChangeRole(String targetAccountId, String oldRole, String newRole, String reason) {
        String actorId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getAccountId()
                : null;

        // 1. Ghi log vào Database để lưu vết
        AuditLogSql.getInstance().log(
                actorId,
                "CHANGE_ROLE",
                "ACCOUNT",
                targetAccountId,
                "role=" + (oldRole != null ? oldRole : "UNKNOWN"),
                "role=" + (newRole != null ? newRole : "UNKNOWN"),
                reason != null ? reason : "Admin cap nhat quyen",
                localIp(),
                deviceInfo()
        );

        // =========================================================
        // 2. THÊM MỚI: BẮN TÍN HIỆU REAL-TIME
        // =========================================================
        try {
            // Gửi lệnh qua WebSocket để đuổi người bị đổi quyền (Logout) 
            // hoặc để các máy khác cập nhật lại bảng danh sách tài khoản
            RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");

            // Gửi lệnh cập nhật nhân viên để bảng hồ sơ nhân viên cũng load lại
            RealtimeClient.send("EMPLOYEES_CHANGED");

            // Bắn sự kiện nội bộ cho máy hiện tại (máy Admin) tự reload UI
            EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "ROLE_CHANGED"));

            System.out.println("[AccountService] Đã kích hoạt thông báo Real-time cho thay đổi quyền hạn.");
        } catch (Exception e) {
            System.err.println("[AccountService] Không thể gửi tín hiệu Real-time: " + e.getMessage());
        }
    }

    private static String localIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String deviceInfo() {
        return System.getProperty("os.name") + " | Java " + System.getProperty("java.version");
    }
}
