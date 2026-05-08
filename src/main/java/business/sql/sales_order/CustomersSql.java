package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import model.order.Customer;

public class CustomersSql implements SqlInterface<Customer> {

    public static CustomersSql getInstance() {
        return new CustomersSql();
    }

    @Override
    public int insert(Customer t) {
        String sql = "INSERT INTO CUSTOMERS "
                + "(CUSTOMER_ID, CUSTOMER_NAME, REWARD_POINTS, IS_DELETED, PHONE, EMAIL, ADDRESS) "
                + "VALUES (?, ?, ?, 0, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            try {
                pst.setString(1, t.getCustomerId());
                pst.setString(2, t.getCustomerName());
                pst.setInt(3, t.getRewardPoints());
                pst.setString(4, t.getPhone());
                pst.setString(5, t.getEmail());
                pst.setString(6, t.getAddress());
                int rows = pst.executeUpdate();
                con.commit();
                return rows;
            } catch (Exception e) {
                con.rollback();
                return 0;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public int update(Customer t) {
        String sql = "UPDATE CUSTOMERS SET CUSTOMER_NAME = ?, REWARD_POINTS = ?, PHONE = ?, EMAIL = ?, ADDRESS = ? WHERE CUSTOMER_ID = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getCustomerName());
            pst.setInt(2, t.getRewardPoints());
            pst.setString(3, t.getPhone());
            pst.setString(4, t.getEmail());
            pst.setString(5, t.getAddress());
            pst.setString(6, t.getCustomerId());
            return pst.executeUpdate();
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        String sql = "UPDATE CUSTOMERS SET IS_DELETED = 1 WHERE CUSTOMER_ID = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public Customer selectById(String id) {
        String sql = "SELECT * FROM CUSTOMERS WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ========================================================
    // 🌟 FIX: TÌM KHÁCH HÀNG QUA SĐT (Đã sửa lỗi viết hoa TOTAL_SPENDING)
    // ========================================================
    public Customer findByPhone(String phone) {
        String sql = "SELECT c.*, "
                + "NVL((SELECT SUM(total_amount) FROM ORDERS o WHERE o.customer_id = c.customer_id AND o.is_deleted = 0), 0) AS TOTAL_SPENDING "
                + "FROM CUSTOMERS c WHERE c.PHONE = ? AND c.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, phone);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Customer c = map(rs);
                    // Dùng chữ IN HOA TOTAL_SPENDING để khớp với Oracle
                    c.setTotalSpending(rs.getDouble("TOTAL_SPENDING"));
                    return c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ArrayList<Customer> selectAll() {
        ArrayList<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Customer> selectByCondition(String condition) {
        ArrayList<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0 " + (condition == null ? "" : condition);
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========================================================
    // 🌟 FIX: LẤY DANH SÁCH KHÁCH KÈM HẠNG (Đã sửa lỗi logic "Đồng")
    // ========================================================
    public List<Customer> selectAllWithRank() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT c.*, "
                + "NVL((SELECT SUM(total_amount) FROM ORDERS o WHERE o.customer_id = c.customer_id AND o.is_deleted = 0), 0) AS TOTAL_SPENDING "
                + "FROM CUSTOMERS c WHERE c.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Customer c = map(rs);
                double spending = rs.getDouble("TOTAL_SPENDING");
                c.setTotalSpending(spending);

                // Logic xếp hạng chuẩn 5 hạng (Khớp hoàn toàn với Customer.java)
                if (spending >= 80000000) {
                    c.setMemberRank("Kim cương");
                } else if (spending >= 40000000) {
                    c.setMemberRank("Vàng");
                } else if (spending >= 15000000) {
                    c.setMemberRank("Bạc");
                } else if (spending >= 5000000) {
                    c.setMemberRank("Đồng");
                } else {
                    c.setMemberRank("Thường"); // Trả về "Thường" nếu dưới 5 triệu
                }
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getString("CUSTOMER_ID"));
        c.setCustomerName(rs.getString("CUSTOMER_NAME"));
        c.setRewardPoints(rs.getInt("REWARD_POINTS"));
        c.setIsDeleted(rs.getInt("IS_DELETED"));
        c.setPhone(rs.getString("PHONE"));
        c.setEmail(rs.getString("EMAIL"));
        c.setAddress(rs.getString("ADDRESS"));
        return c;
    }
}
