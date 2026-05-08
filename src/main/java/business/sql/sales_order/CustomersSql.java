package business.sql.sales_order;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.order.Customer;
import model.order.Order;

public class CustomersSql implements SqlInterface<Customer> {

    public static CustomersSql getInstance() {
        return new CustomersSql();
    }

    // =========================================================
    // INSERT
    // =========================================================
    @Override
    public int insert(Customer t) {

        String sql = "INSERT INTO CUSTOMERS ("
                + "CUSTOMER_ID, CUSTOMER_NAME, REWARD_POINTS, "
                + "TOTAL_SPENDING, MEMBER_RANK, "
                + "IS_DELETED, PHONE, EMAIL, ADDRESS"
                + ") VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, t.getCustomerId());
            pst.setString(2, t.getCustomerName());
            pst.setInt(3, 0);
            pst.setDouble(4, 0);
            pst.setString(5, "Thường");
            pst.setString(6, t.getPhone());
            pst.setString(7, t.getEmail());
            pst.setString(8, t.getAddress());

            return pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // UPDATE
    // =========================================================
    @Override
    public int update(Customer t) {

        String sql = "UPDATE CUSTOMERS SET "
                + "CUSTOMER_NAME = ?, "
                + "PHONE = ?, "
                + "EMAIL = ?, "
                + "ADDRESS = ? "
                + "WHERE CUSTOMER_ID = ?";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, t.getCustomerName());
            pst.setString(2, t.getPhone());
            pst.setString(3, t.getEmail());
            pst.setString(4, t.getAddress());
            pst.setString(5, t.getCustomerId());

            return pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================
    @Override
    public int delete(String id) {

        String sql = "UPDATE CUSTOMERS "
                + "SET IS_DELETED = 1 "
                + "WHERE CUSTOMER_ID = ?";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, id);

            return pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // SELECT BY ID
    // =========================================================
    @Override
    public Customer selectById(String id) {

        String sql = "SELECT * FROM CUSTOMERS "
                + "WHERE CUSTOMER_ID = ? "
                + "AND IS_DELETED = 0";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, id);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // FIND BY PHONE
    // =========================================================
    public Customer findByPhone(String phone) {

        String sql = "SELECT * FROM CUSTOMERS "
                + "WHERE PHONE = ? "
                + "AND IS_DELETED = 0";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, phone);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // SELECT ALL
    // =========================================================
    @Override
    public ArrayList<Customer> selectAll() {

        ArrayList<Customer> list = new ArrayList<>();

        String sql = "SELECT * FROM CUSTOMERS "
                + "WHERE IS_DELETED = 0 "
                + "ORDER BY CUSTOMER_NAME";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // SELECT BY CONDITION
    // =========================================================
    @Override
    public List<Customer> selectByCondition(String condition) {

        ArrayList<Customer> list = new ArrayList<>();

        String sql = "SELECT * FROM CUSTOMERS "
                + "WHERE IS_DELETED = 0 "
                + (condition == null ? "" : condition);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // SELECT ALL WITH RANK
    // =========================================================
    public List<Customer> selectAllWithRank() {

        List<Customer> list = new ArrayList<>();

        String sql = "SELECT * FROM CUSTOMERS "
                + "WHERE IS_DELETED = 0 "
                + "ORDER BY TOTAL_SPENDING DESC";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // UPDATE SAU THANH TOÁN
    // =========================================================
    public void updateCustomerAfterPayment(Connection con, Order order) throws SQLException {

        if (order.getCustomerId() == null
                || order.getCustomerId().isBlank()) {
            return;
        }

        // =====================================================
        // 1. TÍNH TOTAL SPENDING
        // =====================================================
        double totalSpending = 0;

        String spendingSql = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) AS TOTAL "
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

        // =====================================================
        // 2. TÍNH ĐIỂM
        // =====================================================
        int rewardPoints = (int) (totalSpending / 10000);

        // =====================================================
        // 3. TÍNH HẠNG
        // =====================================================
        String rank = calculateRank(totalSpending);

        // =====================================================
        // 4. UPDATE CUSTOMER
        // =====================================================
        String updateSql = "UPDATE CUSTOMERS SET "
                + "TOTAL_SPENDING = ?, "
                + "REWARD_POINTS = ?, "
                + "MEMBER_RANK = ? "
                + "WHERE CUSTOMER_ID = ?";

        try (PreparedStatement pst = con.prepareStatement(updateSql)) {

            pst.setDouble(1, totalSpending);
            pst.setInt(2, rewardPoints);
            pst.setString(3, rank);
            pst.setString(4, order.getCustomerId());

            pst.executeUpdate();
        }
    }

    // =========================================================
    // RECALCULATE KHI HỦY ĐƠN
    // =========================================================
    public void recalculateCustomerRank(Connection con, String customerId) throws SQLException {

        if (customerId == null || customerId.isBlank()) {
            return;
        }

        double totalSpending = 0;

        String spendingSql = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) AS TOTAL "
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

        int rewardPoints = (int) (totalSpending / 10000);

        String rank = calculateRank(totalSpending);

        String updateSql = "UPDATE CUSTOMERS SET "
                + "TOTAL_SPENDING = ?, "
                + "REWARD_POINTS = ?, "
                + "MEMBER_RANK = ? "
                + "WHERE CUSTOMER_ID = ?";

        try (PreparedStatement pst = con.prepareStatement(updateSql)) {

            pst.setDouble(1, totalSpending);
            pst.setInt(2, rewardPoints);
            pst.setString(3, rank);
            pst.setString(4, customerId);

            pst.executeUpdate();
        }
    }

    // =========================================================
    // TÍNH HẠNG
    // =========================================================
    private String calculateRank(double spending) {

        if (spending >= 80000000) {
            return "Kim cương";
        }

        if (spending >= 40000000) {
            return "Vàng";
        }

        if (spending >= 15000000) {
            return "Bạc";
        }

        if (spending >= 5000000) {
            return "Đồng";
        }

        return "Thường";
    }

    // =========================================================
    // MAP RESULTSET
    // =========================================================
    private Customer map(ResultSet rs) throws SQLException {

        Customer c = new Customer();

        c.setCustomerId(rs.getString("CUSTOMER_ID"));
        c.setCustomerName(rs.getString("CUSTOMER_NAME"));
        c.setRewardPoints(rs.getInt("REWARD_POINTS"));
        c.setTotalSpending(rs.getDouble("TOTAL_SPENDING"));
        c.setMemberRank(rs.getString("MEMBER_RANK"));
        c.setPhone(rs.getString("PHONE"));
        c.setEmail(rs.getString("EMAIL"));
        c.setAddress(rs.getString("ADDRESS"));
        c.setIsDeleted(rs.getInt("IS_DELETED"));

        return c;
    }
}
