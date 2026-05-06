package business.service;

import business.sql.rbac.StoreManagerSql;
import java.util.List;
import model.account.StoreManagerDTO;

public class StoreManagerService {

    private final StoreManagerSql managerSql = StoreManagerSql.getInstance();

    // Hàm này để UI gọi lấy dữ liệu đổ vào bảng
    public List<StoreManagerDTO> getStoreManagerList() {
        return managerSql.getAllStoreManagers();
    }
}
