package view;

import view.components.AdminDataPanel;

public class AccountRoleAssignmentPanel extends AdminDataPanel {
    public AccountRoleAssignmentPanel() {
        super("Quản lý tài khoản", "Giám sát tài khoản, trạng thái đăng nhập và nhóm quyền",
            new String[]{"Tổng tài khoản","Đang hoạt động","Bị khóa","Đang online"},
            new String[]{"SELECT COUNT(*) FROM ACCOUNTS WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM ACCOUNTS WHERE NVL(is_deleted,0)=0 AND UPPER(NVL(status,'ACTIVE')) IN ('ACTIVE','HOẠT ĐỘNG')","SELECT COUNT(*) FROM ACCOUNTS WHERE NVL(is_deleted,0)=0 AND UPPER(NVL(status,'ACTIVE')) IN ('LOCKED','BỊ KHÓA')","SELECT COUNT(*) FROM ACCOUNTS WHERE NVL(is_deleted,0)=0 AND NVL(active_sessions,0)>0"},
            "Danh sách tài khoản",
            "SELECT a.account_id, a.username, u.full_name, NVL(a.status,'ACTIVE'), NVL(a.online_status,'OFFLINE'), NVL(a.active_sessions,0) FROM ACCOUNTS a LEFT JOIN USERS u ON a.user_id=u.user_id WHERE NVL(a.is_deleted,0)=0 ORDER BY a.account_id",
            new String[]{"Mã TK","Tên đăng nhập","Người dùng","Trạng thái","Online","Sessions"}
        );
    }
}
