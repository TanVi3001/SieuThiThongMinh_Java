package business.sql.rbac;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.account.StoreManagerDTO;

public class StoreManagerSql {
    
    private static StoreManagerSql instance;

    public static StoreManagerSql getInstance() {
        if (instance == null) {
            instance = new StoreManagerSql();
        }
        return instance;
    }

    public List<StoreManagerDTO> getAllStoreManagers() {
        List<StoreManagerDTO> list = new ArrayList<>();
        
        // Câu SQL thần thánh: Lấy thông tin NV và kiểm tra xem có tài khoản chưa bằng LEFT JOIN
        String sql = "SELECT e.employee_id, e.employee_name, e.phone, e.email, e.gender, "
                   + "CASE WHEN a.user_id IS NOT NULL THEN N'Đã cấp' ELSE N'Chưa cấp' END AS account_status "
                   + "FROM EMPLOYEES e "
                   + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                   + "WHERE e.role_id = 'R_STORE_MNG' " // Lọc ra Cửa hàng trưởng
                   + "AND NVL(e.is_deleted, 0) = 0 "    // Lọc bỏ nhân viên đã bị xóa (Soft Delete)
                   + "ORDER BY e.employee_id DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
             
            while (rs.next()) {
                StoreManagerDTO dto = new StoreManagerDTO(
                    rs.getString("employee_id"),
                    rs.getString("employee_name"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("gender") != null ? rs.getString("gender") : "Chưa cập nhật",
                    rs.getString("account_status")
                );
                list.add(dto);
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi lấy danh sách Cửa hàng trưởng: " + ex.getMessage());
            ex.printStackTrace();
        }
        return list;
    }
}