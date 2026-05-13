package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import model.order.DeliveryManagement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DeliveryManagementSql implements SqlInterface<DeliveryManagement> {

    private static DeliveryManagementSql instance;

    private DeliveryManagementSql() {
    }

    public static DeliveryManagementSql getInstance() {
        if (instance == null) {
            instance = new DeliveryManagementSql();
        }
        return instance;
    }

    @Override
    public int insert(DeliveryManagement t) {
        int ketQua = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO DELIVERY_MANAGEMENT (delivery_id, order_id, employee_id, execution_date, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, t.getDeliveryId());
            pst.setString(2, t.getOrderId());
            pst.setString(3, t.getEmployeeId());
            pst.setDate(4, t.getExecutionDate());
            pst.setString(5, t.getStatus());
            pst.setInt(6, t.getIsDeleted());

            ketQua = pst.executeUpdate();
            DatabaseConnection.closeConnection(con);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ketQua;
    }

    @Override
    public int update(DeliveryManagement t) {
        int ketQua = 0;
        try {
            java.sql.Connection con = common.db.DatabaseConnection.getConnection();
            String sql = "UPDATE DELIVERY_MANAGEMENT SET order_id=?, employee_id=?, execution_date=?, status=?, is_deleted=? WHERE delivery_id=?";
            java.sql.PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, t.getOrderId());
            pst.setString(2, t.getEmployeeId());
            pst.setDate(3, t.getExecutionDate());
            pst.setString(4, t.getStatus());
            pst.setInt(5, t.getIsDeleted());
            pst.setString(6, t.getDeliveryId());

            ketQua = pst.executeUpdate();
            common.db.DatabaseConnection.closeConnection(con);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ketQua;
    }

    @Override
    public int delete(String id) {
        int ketQua = 0;
        try {
            java.sql.Connection con = common.db.DatabaseConnection.getConnection();
            String sql = "UPDATE DELIVERY_MANAGEMENT SET is_deleted = 1 WHERE delivery_id = ?";
            java.sql.PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, id);
            ketQua = pst.executeUpdate();
            common.db.DatabaseConnection.closeConnection(con);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ketQua;
    }

    @Override
    public ArrayList<DeliveryManagement> selectAll() {
        ArrayList<DeliveryManagement> ketQua = new ArrayList<>();
        String sql = "SELECT * FROM DELIVERY_MANAGEMENT WHERE is_deleted = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                DeliveryManagement dm = new DeliveryManagement();

                // Gán dữ liệu bằng các hàm set để tránh phụ thuộc vào thứ tự tham số Constructor
                dm.setDeliveryId(rs.getString("delivery_id"));
                dm.setOrderId(rs.getString("order_id"));
                dm.setEmployeeId(rs.getString("employee_id"));
                dm.setExecutionDate(rs.getDate("execution_date"));
                dm.setStatus(rs.getString("status"));
                dm.setIsDeleted(rs.getInt("is_deleted"));

                ketQua.add(dm);
            }
        } catch (Exception e) {
            // In lỗi chi tiết để dễ debug trong quá trình làm đồ án
            System.err.println("Lỗi tại DeliveryManagementSql.selectAll: " + e.getMessage());
            e.printStackTrace();
        }
        return ketQua;
    }

    @Override
    public DeliveryManagement selectById(String id) {
        DeliveryManagement ketQua = null;
        try {
            java.sql.Connection con = common.db.DatabaseConnection.getConnection();
            // Vẫn phải check is_deleted = 0 để đảm bảo không tìm thấy thằng đã bị "xóa"
            String sql = "SELECT * FROM DELIVERY_MANAGEMENT WHERE delivery_id = ? AND is_deleted = 0";
            java.sql.PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, id);

            java.sql.ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                ketQua = new DeliveryManagement(
                        rs.getString("delivery_id"),
                        rs.getString("order_id"),
                        rs.getString("employee_id"),
                        rs.getDate("execution_date"),
                        rs.getString("status"),
                        rs.getInt("is_deleted")
                );
            }
            common.db.DatabaseConnection.closeConnection(con);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ketQua;
    }

    @Override
    public List<DeliveryManagement> selectByCondition(String condition) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    public Map<String, String> getDeliveryInfoByOrderId(String orderId) {
        Map<String, String> result = new HashMap<>();

        // Câu SQL thần thánh dùng LEFT JOIN để cover cả 3 trường hợp giao hàng
        String sql = "SELECT "
                + "    dm.delivery_id, dm.status, TO_CHAR(dm.execution_date, 'DD/MM/YYYY') as exec_date, "
                + "    e.employee_name || ' (' || dm.employee_id || ')' AS nv_phu_trach, "
                + "    CASE "
                + "        WHEN hd.delivery_id IS NOT NULL THEN 'Giao hàng tận nơi' "
                + "        WHEN sp.delivery_id IS NOT NULL THEN 'Lấy tại tủ khóa' "
                + "        WHEN op.delivery_id IS NOT NULL THEN 'Nhận tại quầy' "
                + "        ELSE 'Chưa xác định' "
                + "    END AS loai_giao_hang, "
                + "    hd.delivery_address, hd.shipping_fee, hd.recipient_phone, "
                + "    sp.locker_id, TO_CHAR(sp.pickup_appointment, 'DD/MM/YYYY HH24:MI') as pickup_time, "
                + "    op.counter_position "
                + "FROM DELIVERY_MANAGEMENT dm "
                + "LEFT JOIN EMPLOYEES e ON dm.employee_id = e.employee_id "
                + "LEFT JOIN HOME_DELIVERY hd ON dm.delivery_id = hd.delivery_id "
                + "LEFT JOIN STORE_PICKUP sp ON dm.delivery_id = sp.delivery_id "
                + "LEFT JOIN ON_SITE_PICKUP op ON dm.delivery_id = op.delivery_id "
                + "WHERE dm.order_id = ? AND dm.is_deleted = 0";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection();
             java.sql.PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, orderId);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    result.put("delivery_id", rs.getString("delivery_id"));
                    result.put("status", rs.getString("status"));
                    result.put("execution_date", rs.getString("exec_date"));
                    result.put("nv_phu_trach", rs.getString("nv_phu_trach"));

                    String loaiGiaoHang = rs.getString("loai_giao_hang");
                    result.put("loai_giao_hang", loaiGiaoHang);

                    // Lấy thông tin dựa theo loại
                    if ("Giao hàng tận nơi".equals(loaiGiaoHang)) {
                        result.put("address", rs.getString("delivery_address"));
                        result.put("fee", rs.getString("shipping_fee"));
                        result.put("phone", rs.getString("recipient_phone"));
                    } else if ("Lấy tại tủ khóa".equals(loaiGiaoHang)) {
                        result.put("locker", rs.getString("locker_id"));
                        result.put("time", rs.getString("pickup_time"));
                    } else if ("Nhận tại quầy".equals(loaiGiaoHang)) {
                        result.put("counter", rs.getString("counter_position"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải thông tin giao hàng: " + e.getMessage());
        }
        return result;
    }
}
