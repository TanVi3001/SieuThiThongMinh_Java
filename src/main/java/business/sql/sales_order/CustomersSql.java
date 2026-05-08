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
import model.order.Order;

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
    // Bước 1: Tạo hàm dùng chung để tính hạng dựa trên chi tiêu

    public void applyMemberRank(Customer c) {
        double spending = c.getTotalSpending();
        if (spending >= 80000000) {
            c.setMemberRank("Kim cương");
        } else if (spending >= 40000000) {
            c.setMemberRank("Vàng");
        } else if (spending >= 15000000) {
            c.setMemberRank("Bạc");
        } else if (spending >= 5000000) {
            c.setMemberRank("Đồng");
        } else {
            c.setMemberRank("Thường");
        }
    }

    // ========================================================
    // 🌟 FIX: TÌM KHÁCH HÀNG QUA SĐT (Đã sửa lỗi viết hoa TOTAL_SPENDING)
    // ========================================================
    // Bước 2: Cập nhật hàm findByPhone để áp dụng hạng
    public Customer findByPhone(String phone) {

        String sql = "SELECT c.*, "
                + "NVL((SELECT SUM(o.total_amount) "
                + "FROM ORDERS o "
                + "WHERE o.customer_id = c.customer_id "
                + "AND o.is_deleted = 0 "
                + "AND o.status = 'Hoàn thành'), 0) AS TOTAL_SPENDING "
                + "FROM CUSTOMERS c "
                + "WHERE c.PHONE = ? "
                + "AND c.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, phone);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {

                    Customer c = map(rs);

                    double spending = rs.getDouble("TOTAL_SPENDING");

                    c.setTotalSpending(spending);

                    applyMemberRank(c);

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
                + "NVL((SELECT SUM(o.total_amount) "
                + "FROM ORDERS o "
                + "WHERE o.customer_id = c.customer_id "
                + "AND o.is_deleted = 0 "
                + "AND o.status = 'Hoàn thành'), 0) AS TOTAL_SPENDING "
                + "FROM CUSTOMERS c "
                + "WHERE c.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                Customer c = map(rs);

                double spending = rs.getDouble("TOTAL_SPENDING");

                c.setTotalSpending(spending);

                applyMemberRank(c);

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
        try {
            c.setMemberRank(rs.getString("MEMBER_RANK"));
        } catch (Exception e) {
        }
        return c;
    }

    public int addCustomerSpending(String customerId, double amount) {

        String sql
                = "UPDATE CUSTOMERS "
                + "SET REWARD_POINTS = NVL(REWARD_POINTS,0) + ? "
                + "WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, (int) (amount / 1000)); // ví dụ: 1000đ = 1 điểm
            pst.setString(2, customerId);

            return pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public void updateCustomerAfterPayment(Connection con, Order order) throws SQLException {

        // Không có khách hàng thành viên
        if (order.getCustomerId() == null || order.getCustomerId().isBlank()) {
            return;
        }

        // ==================================================
        // 1. CỘNG ĐIỂM THƯỞNG
        // ==================================================
        int earnedPoints = (int) (order.getTotalAmount() / 10000);

        String pointSql
                = "UPDATE CUSTOMERS "
                + "SET REWARD_POINTS = NVL(REWARD_POINTS, 0) + ? "
                + "WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        try (PreparedStatement pst = con.prepareStatement(pointSql)) {

            pst.setInt(1, earnedPoints);
            pst.setString(2, order.getCustomerId());

            pst.executeUpdate();
        }

        // ==================================================
        // 2. TÍNH TỔNG CHI TIÊU
        // ==================================================
        double totalSpending = 0;

        String spendingSql
                = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) AS TOTAL "
                + "FROM ORDERS "
                + "WHERE CUSTOMER_ID = ? "
                + "AND NVL(IS_DELETED, 0) = 0 "
                + "AND STATUS = 'Hoàn thành'";

        try (PreparedStatement pst = con.prepareStatement(spendingSql)) {

            pst.setString(1, order.getCustomerId());

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    totalSpending = rs.getDouble("TOTAL");
                }
            }
        }

        // ==================================================
        // 3. XÁC ĐỊNH HẠNG
        // ==================================================
        String rank;

        if (totalSpending >= 80000000) {
            rank = "Kim cương";
        } else if (totalSpending >= 40000000) {
            rank = "Vàng";
        } else if (totalSpending >= 15000000) {
            rank = "Bạc";
        } else if (totalSpending >= 5000000) {
            rank = "Đồng";
        } else {
            rank = "Thường";
        }

        // ==================================================
        // 4. UPDATE HẠNG
        // ==================================================
        String rankSql
                = "UPDATE CUSTOMERS "
                + "SET MEMBER_RANK = ? "
                + "WHERE CUSTOMER_ID = ?";

        try (PreparedStatement pst = con.prepareStatement(rankSql)) {

            pst.setString(1, rank);
            pst.setString(2, order.getCustomerId());

            pst.executeUpdate();
        }
    }

    public void recalculateCustomerRank(Connection con, String customerId) throws SQLException {

        if (customerId == null || customerId.isBlank()) {
            return;
        }

        double totalSpending = 0;

        String spendingSql
                = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) AS TOTAL "
                + "FROM ORDERS "
                + "WHERE CUSTOMER_ID = ? "
                + "AND NVL(IS_DELETED, 0) = 0 "
                + "AND STATUS = 'Hoàn thành'";

        try (PreparedStatement pst = con.prepareStatement(spendingSql)) {

            pst.setString(1, customerId);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    totalSpending = rs.getDouble("TOTAL");
                }
            }
        }

        String rank;

        if (totalSpending >= 80000000) {
            rank = "Kim cương";
        } else if (totalSpending >= 40000000) {
            rank = "Vàng";
        } else if (totalSpending >= 15000000) {
            rank = "Bạc";
        } else if (totalSpending >= 5000000) {
            rank = "Đồng";
        } else {
            rank = "Thường";
        }

        String sql
                = "UPDATE CUSTOMERS "
                + "SET MEMBER_RANK = ? "
                + "WHERE CUSTOMER_ID = ?";

        try (PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, rank);
            pst.setString(2, customerId);

            pst.executeUpdate();
        }
    }
}
