package view;

import view.components.AdminDataPanel;

public class RoleManagementPanel extends AdminDataPanel {
    public RoleManagementPanel() {
        super("Quản lý phân quyền", "Quản lý nhóm quyền, chức năng và ma trận quyền RBAC",
            new String[]{"Nhóm quyền","Vai trò","Chức năng","Quyền export"},
            new String[]{"SELECT COUNT(*) FROM ROLE_GROUPS WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM ROLES WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM FUNCTIONS WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM ROLES WHERE NVL(is_deleted,0)=0 AND NVL(can_export,0)=1"},
            "Ma trận quyền",
            "SELECT r.role_id, NVL(rg.group_name,'Chưa gán'), f.function_name, r.can_view, r.can_add, r.can_edit, r.can_delete, r.can_export FROM ROLES r LEFT JOIN FUNCTIONS f ON r.function_id=f.function_id LEFT JOIN ROLE_GROUP_ASSIGN_ROLE gr ON r.role_id=gr.role_id AND NVL(gr.is_deleted,0)=0 LEFT JOIN ROLE_GROUPS rg ON gr.role_group_id=rg.role_group_id WHERE NVL(r.is_deleted,0)=0 ORDER BY rg.group_name, f.function_name",
            new String[]{"Mã quyền","Nhóm quyền","Chức năng","View","Add","Edit","Delete","Export"}
        );
    }
}
