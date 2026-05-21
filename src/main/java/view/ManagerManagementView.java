package view;

import view.components.AdminDataPanel;

public class ManagerManagementView extends AdminDataPanel {
    public ManagerManagementView() {
        super("Quản lý cửa hàng trưởng", "Quản lý hồ sơ, chi nhánh công tác và trạng thái cấp tài khoản",
            new String[]{"Tổng quản lý","Đã có tài khoản","Chưa phân chi nhánh","Đang hoạt động"},
            new String[]{"SELECT COUNT(*) FROM EMPLOYEES WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM EMPLOYEES e JOIN ACCOUNTS a ON a.user_id=e.employee_id WHERE NVL(e.is_deleted,0)=0 AND NVL(a.is_deleted,0)=0","SELECT COUNT(*) FROM EMPLOYEES WHERE NVL(is_deleted,0)=0 AND store_id IS NULL","SELECT COUNT(*) FROM EMPLOYEES WHERE NVL(is_deleted,0)=0"},
            "Danh sách cửa hàng trưởng",
            "SELECT e.employee_id, e.employee_name, NVL(s.store_name, NVL(s.address, e.store_id)), e.phone, e.email, CASE WHEN a.account_id IS NULL THEN 'Chưa cấp' ELSE 'Đã cấp' END, e.gender FROM EMPLOYEES e LEFT JOIN STORES s ON e.store_id=s.store_id LEFT JOIN ACCOUNTS a ON a.user_id=e.employee_id AND NVL(a.is_deleted,0)=0 WHERE NVL(e.is_deleted,0)=0 ORDER BY e.employee_id",
            new String[]{"Mã QL","Họ và tên","Chi nhánh","SĐT","Email","Cấp tài khoản","Giới tính"}
        );
    }
}
