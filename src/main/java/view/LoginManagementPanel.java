package view;

import view.components.AdminDataPanel;

public class LoginManagementPanel extends AdminDataPanel {
    public LoginManagementPanel() {
        super("Lịch sử truy cập", "Theo dõi phiên đăng nhập, trạng thái truy cập và cảnh báo bảo mật",
            new String[]{"Lượt truy cập","Đang online","Thành công","Thất bại"},
            new String[]{"SELECT COUNT(*) FROM LOGIN_HISTORY WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM ACCOUNTS WHERE NVL(is_deleted,0)=0 AND NVL(active_sessions,0)>0","SELECT COUNT(*) FROM LOGIN_HISTORY WHERE NVL(is_deleted,0)=0 AND UPPER(NVL(status,'SUCCESS'))='SUCCESS'","SELECT COUNT(*) FROM LOGIN_HISTORY WHERE NVL(is_deleted,0)=0 AND UPPER(NVL(status,'SUCCESS'))<>'SUCCESS'"},
            "Danh sách truy cập",
            "SELECT l.log_id, a.username, l.ip_address, l.device_info, l.login_time, NVL(l.status,'SUCCESS') FROM LOGIN_HISTORY l LEFT JOIN ACCOUNTS a ON l.account_id=a.account_id WHERE NVL(l.is_deleted,0)=0 ORDER BY l.login_time DESC",
            new String[]{"Mã log","Tài khoản","IP Address","Thiết bị","Thời gian","Trạng thái"}
        );
    }
}
