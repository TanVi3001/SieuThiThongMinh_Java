package view;

import view.components.AdminDataPanel;

public class AuditLogPanel extends AdminDataPanel {
    public AuditLogPanel() {
        super("Nhật ký hệ thống", "Audit trail toàn hệ thống cho thao tác thêm, sửa, xóa và đăng nhập",
            new String[]{"Tổng thao tác","Thêm mới","Cập nhật","Xóa"},
            new String[]{"SELECT COUNT(*) FROM AUDIT_LOG WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM AUDIT_LOG WHERE NVL(is_deleted,0)=0 AND UPPER(action_type) LIKE '%CREATE%'","SELECT COUNT(*) FROM AUDIT_LOG WHERE NVL(is_deleted,0)=0 AND UPPER(action_type) LIKE '%UPDATE%'","SELECT COUNT(*) FROM AUDIT_LOG WHERE NVL(is_deleted,0)=0 AND UPPER(action_type) LIKE '%DELETE%'"},
            "Nhật ký thao tác",
            "SELECT created_at, account_id, ip_address, action_type, entity_type, entity_id, NVL(new_value, old_value) FROM AUDIT_LOG WHERE NVL(is_deleted,0)=0 ORDER BY created_at DESC",
            new String[]{"Thời gian","Tài khoản","IP Address","Hành động","Đối tượng","Mã đối tượng","Chi tiết"}
        );
    }
}
