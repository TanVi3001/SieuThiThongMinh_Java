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

    @Override
    public int insert(Customer t) {
        String sql = "INSERT INTO CUSTOMERS ("
                + "CUSTOMER_ID, CUSTOMER_NAME, REWARD_POINTS, "
                + "TOTAL_SPENDING, MEMBER_RANK, "
                + "IS_DELETED, PHONE, EMAIL, ADDRESS"
                + ") VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    @Override
    public int update(Customer t) {
        String sql = "UPDATE CUSTOMERS SET CUSTOMER_NAME = ?, PHONE = ?, EMAIL = ?, ADDRESS = ? WHERE CUSTOMER_ID = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    @Override
    public int delete(String id) {
        String sql = "UPDATE CUSTOMERS SET IS_DELETED = 1 WHERE CUSTOMER_ID = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Customer findByPhone(String phone) {
        String sql = "SELECT * FROM CUSTOMERS WHERE PHONE = ? AND IS_DELETED = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
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

    @Override
    public ArrayList<Customer> selectAll() {
        ArrayList<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0 ORDER BY CUSTOMER_NAME";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Customer> selectAllWithRank() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0 ORDER BY TOTAL_SPENDING DESC";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * CUSTOMERS là global toàn chuỗi. Method này chỉ scope CHỈ SỐ PHÂN TÍCH theo ORDERS.store_id,
     * không tách dữ liệu khách hàng theo chi nhánh và không yêu cầu CUSTOMERS.store_id.
     */
    public List<Customer> selectAllWithRankForStoreAnalytics(String storeId) {
        if (storeId == null || storeId.trim().isEmpty()) {
            return selectAllWithRank();
        }

        List<Customer> list = new ArrayList<>();
        String sql = """
            SELECT
                c.customer_id,
                c.customer_name,
                c.reward_points,
                NVL(SUM(CASE
                    WHEN o.store_id = ?
                     AND NVL(o.is_deleted, 0) = 0
                     AND o.status = N'Hoàn thành'
                    THEN o.total_amount ELSE 0 END), 0) AS total_spending,
                c.member_rank,
                c.is_deleted,
                c.phone,
                c.email,
                c.address
            FROM CUSTOMERS c
            LEFT JOIN ORDERS o
                   ON o.customer_id = c.customer_id
            WHERE NVL(c.is_deleted, 0) = 0
            GROUP BY c.customer_id, c.customer_name, c.reward_points,
                     c.member_rank, c.is_deleted, c.phone, c.email, c.address
            HAVING NVL(SUM(CASE
                    WHEN o.store_id = ?
                     AND NVL(o.is_deleted, 0) = 0
                     AND o.status = N'Hoàn thành'
                    THEN o.total_amount ELSE 0 END), 0) > 0
            ORDER BY total_spending DESC
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, storeId.trim());
            pst.setString(2, storeId.trim());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countNewCustomersByFirstPurchaseInStoreThisMonth(String storeId) {
        if (storeId == null || storeId.trim().isEmpty()) {
            return 0;
        }
        String sql = """
            SELECT COUNT(*) AS total
            FROM (
                SELECT customer_id, MIN(order_date) AS first_order_date
                FROM ORDERS
                WHERE customer_id IS NOT NULL
                  AND store_id = ?
                  AND NVL(is_deleted, 0) = 0
                  AND status = N'Hoàn thành'
                GROUP BY customer_id
            ) x
            WHERE x.first_order_date >= TRUNC(SYSDATE, 'MM')
              AND x.first_order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
        """;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, storeId.trim());
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void updateCustomerAfterPayment(Connection con, Order order) throws SQLException {
        if (order.getCustomerId() == null || order.getCustomerId().isBlank()) {
            return;
        }
        recalculateCustomerRank(con, order.getCustomerId());
    }

    public void recalculateCustomerRank(Connection con, String customerId) throws SQLException {
        if (customerId == null || customerId.isBlank()) {
            return;
        }

        double totalSpending = 0;
        String spendingSql = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) AS TOTAL "
                + "FROM ORDERS "
                + "WHERE CUSTOMER_ID = ? "
                + "AND NVL(IS_DELETED, 0) = 0 "
                + "AND STATUS = N'Hoàn thành'";

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

        String updateSql = "UPDATE CUSTOMERS SET TOTAL_SPENDING = ?, REWARD_POINTS = ?, MEMBER_RANK = ? WHERE CUSTOMER_ID = ?";
        try (PreparedStatement pst = con.prepareStatement(updateSql)) {
            pst.setDouble(1, totalSpending);
            pst.setInt(2, rewardPoints);
            pst.setString(3, rank);
            pst.setString(4, customerId);
            pst.executeUpdate();
        }
    }

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
