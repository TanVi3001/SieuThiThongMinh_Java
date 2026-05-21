package view;

import view.components.AdminDataPanel;

public class StoreManagementPanel extends AdminDataPanel {
    public StoreManagementPanel() {
        super("Quản lý chi nhánh", "Theo dõi thông tin, trạng thái và dữ liệu vận hành từng siêu thị",
            new String[]{"Tổng chi nhánh","Đang hoạt động","Tạm ngưng"},
            new String[]{"SELECT COUNT(*) FROM STORES WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM STORES WHERE NVL(is_deleted,0)=0 AND NVL(status,'Hoạt động')='Hoạt động'","SELECT COUNT(*) FROM STORES WHERE NVL(is_deleted,0)=0 AND NVL(status,'Hoạt động')<>'Hoạt động'"},
            "Danh sách chi nhánh",
            "SELECT store_id, NVL(store_name, NVL(address, store_id)), phone_number, address, NVL(status,'Hoạt động') FROM STORES WHERE NVL(is_deleted,0)=0 ORDER BY store_id",
            new String[]{"Mã cửa hàng","Tên siêu thị","Số điện thoại","Địa chỉ","Trạng thái"}
        );
    }
}
