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
                + "(CUSTOMER_ID, CUSTOMER_NAME, ROLE_ID, REWARD_POINTS, IS_DELETED, PHONE, EMAIL, ADDRESS) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            try {
                pst.setString(1, t.getCustomerId());
                pst.setString(2, t.getCustomerName());
                pst.setString(3, t.getRoleId());
                pst.setInt(4, t.getRewardPoints());
                pst.setString(5, t.getPhone());
                pst.setString(6, t.getEmail());
                pst.setString(7, t.getAddress());

                int rows = pst.executeUpdate();

                if (rows > 0) {
                    String newValue = joinPairs(
                            pair("customer_name", t.getCustomerName()),
                            pair("role_id", t.getRoleId()),
                            pair("reward_points", t.getRewardPoints()),
                            pair("phone", t.getPhone()),
                            pair("email", t.getEmail()),
                            pair("address", t.getAddress()),
                            pair("is_deleted", 0)
                    );

                    logAuditWithConn(
                            con,
                            "CREATE_CUSTOMER",
                            "CUSTOMER",
                            t.getCustomerId(),
                            null,
                            newValue,
                            "Tao moi khach hang"
                    );
                }

                con.commit();
                return rows;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Insert lỗi: " + e.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                return 0;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Insert lỗi: " + e.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    @Override
    public int update(Customer t) {
        String sql = "UPDATE CUSTOMERS SET "
                + "CUSTOMER_NAME = ?, ROLE_ID = ?, REWARD_POINTS = ?, PHONE = ?, EMAIL = ?, ADDRESS = ? "
                + "WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        String sqlOld = "SELECT CUSTOMER_NAME, ROLE_ID, REWARD_POINTS, PHONE, EMAIL, ADDRESS "
                + "FROM CUSTOMERS WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            try {
                String oldName = null, oldRole = null, oldPhone = null, oldEmail = null, oldAddress = null;
                Integer oldPoints = null;

                try (PreparedStatement psOld = con.prepareStatement(sqlOld)) {
                    psOld.setString(1, t.getCustomerId());
                    try (ResultSet rs = psOld.executeQuery()) {
                        if (rs.next()) {
                            oldName = rs.getString("CUSTOMER_NAME");
                            oldRole = rs.getString("ROLE_ID");
                            oldPoints = rs.getInt("REWARD_POINTS");
                            oldPhone = rs.getString("PHONE");
                            oldEmail = rs.getString("EMAIL");
                            oldAddress = rs.getString("ADDRESS");
                        }
                    }
                }

                pst.setString(1, t.getCustomerName());
                pst.setString(2, t.getRoleId());
                pst.setInt(3, t.getRewardPoints());
                pst.setString(4, t.getPhone());
                pst.setString(5, t.getEmail());
                pst.setString(6, t.getAddress());
                pst.setString(7, t.getCustomerId());

                int rows = pst.executeUpdate();

                if (rows > 0) {
                    String oldValue = joinPairs(
                            diff(oldName, t.getCustomerName()) ? pair("customer_name", oldName) : null,
                            diff(oldRole, t.getRoleId()) ? pair("role_id", oldRole) : null,
                            diff(oldPoints, t.getRewardPoints()) ? pair("reward_points", oldPoints) : null,
                            diff(oldPhone, t.getPhone()) ? pair("phone", oldPhone) : null,
                            diff(oldEmail, t.getEmail()) ? pair("email", oldEmail) : null,
                            diff(oldAddress, t.getAddress()) ? pair("address", oldAddress) : null
                    );

                    String newValue = joinPairs(
                            diff(oldName, t.getCustomerName()) ? pair("customer_name", t.getCustomerName()) : null,
                            diff(oldRole, t.getRoleId()) ? pair("role_id", t.getRoleId()) : null,
                            diff(oldPoints, t.getRewardPoints()) ? pair("reward_points", t.getRewardPoints()) : null,
                            diff(oldPhone, t.getPhone()) ? pair("phone", t.getPhone()) : null,
                            diff(oldEmail, t.getEmail()) ? pair("email", t.getEmail()) : null,
                            diff(oldAddress, t.getAddress()) ? pair("address", t.getAddress()) : null
                    );

                    if (newValue != null && !newValue.isBlank()) {
                        logAuditWithConn(
                                con,
                                "UPDATE_CUSTOMER",
                                "CUSTOMER",
                                t.getCustomerId(),
                                oldValue,
                                newValue,
                                "Cap nhat thong tin khach hang"
                        );
                    }
                }

                con.commit();
                return rows;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Update lỗi: " + e.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                return 0;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Update lỗi: " + e.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        String sql = "UPDATE CUSTOMERS SET IS_DELETED = 1 WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            try {
                pst.setString(1, id);
                int rows = pst.executeUpdate();

                if (rows > 0) {
                    logAuditWithConn(
                            con,
                            "DELETE_CUSTOMER",
                            "CUSTOMER",
                            id,
                            pair("is_deleted", 0),
                            pair("is_deleted", 1),
                            "Xoa mem khach hang"
                    );
                }

                con.commit();
                return rows;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Delete lỗi: " + e.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                return 0;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Delete lỗi: " + e.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
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
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0 "
                + (condition == null ? "" : condition);

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Customer> search(String keyword) {
        ArrayList<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS WHERE IS_DELETED = 0 AND ("
                + "LOWER(CUSTOMER_ID) LIKE ? OR "
                + "LOWER(CUSTOMER_NAME) LIKE ? OR "
                + "LOWER(NVL(PHONE, '')) LIKE ? OR "
                + "LOWER(NVL(EMAIL, '')) LIKE ? OR "
                + "LOWER(NVL(ADDRESS, '')) LIKE ? OR "
                + "LOWER(NVL(ROLE_ID, '')) LIKE ?"
                + ")";

        String k = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, k);
            pst.setString(2, k);
            pst.setString(3, k);
            pst.setString(4, k);
            pst.setString(5, k);
            pst.setString(6, k);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int updateRewardPoints(String customerId, int points) {
        String sql = "UPDATE CUSTOMERS SET REWARD_POINTS = ? WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";
        String sqlOld = "SELECT REWARD_POINTS FROM CUSTOMERS WHERE CUSTOMER_ID = ? AND IS_DELETED = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            try {
                Integer oldPoints = null;
                try (PreparedStatement psOld = con.prepareStatement(sqlOld)) {
                    psOld.setString(1, customerId);
                    try (ResultSet rs = psOld.executeQuery()) {
                        if (rs.next()) {
                            oldPoints = rs.getInt("REWARD_POINTS");
                        }
                    }
                }

                pst.setInt(1, points);
                pst.setString(2, customerId);
                int rows = pst.executeUpdate();

                if (rows > 0 && diff(oldPoints, points)) {
                    logAuditWithConn(
                            con,
                            "UPDATE_CUSTOMER_POINTS",
                            "CUSTOMER",
                            customerId,
                            pair("reward_points", oldPoints),
                            pair("reward_points", points),
                            "Cap nhat diem tich luy khach hang"
                    );
                }

                con.commit();
                return rows;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return 0;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getString("CUSTOMER_ID"));
        c.setCustomerName(rs.getString("CUSTOMER_NAME"));
        c.setRoleId(rs.getString("ROLE_ID"));
        c.setRewardPoints(rs.getInt("REWARD_POINTS"));
        c.setIsDeleted(rs.getInt("IS_DELETED"));
        c.setPhone(rs.getString("PHONE"));
        c.setEmail(rs.getString("EMAIL"));
        c.setAddress(rs.getString("ADDRESS"));
        return c;
    }

    // ===== audit helpers =====
    private boolean diff(Object oldV, Object newV) {
        String o = oldV == null ? null : String.valueOf(oldV).trim();
        String n = newV == null ? null : String.valueOf(newV).trim();
        return (o == null && n != null) || (o != null && !o.equals(n));
    }

    private String pair(String col, Object val) {
        return col + "=" + (val == null ? "null" : String.valueOf(val).trim());
    }

    private String joinPairs(String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts != null) {
            for (String p : parts) {
                if (p != null && !p.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(p);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void logAuditWithConn(Connection con, String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason) throws SQLException {
        model.account.AuditLog log = new model.account.AuditLog();
        log.setAccountId(
                business.service.SessionManager.getCurrentUser() != null
                ? business.service.SessionManager.getCurrentUser().getAccountId()
                : null
        );
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setIpAddress("local");
        log.setDeviceInfo(System.getProperty("os.name") + " | Java " + System.getProperty("java.version"));

        int ar = business.sql.rbac.AuditLogSql.getInstance().insertWithConn(con, log);
        System.out.println("AUDIT CUSTOMER rows=" + ar + ", action=" + actionType + ", entityId=" + entityId);
    }

    /**
     * Lấy danh sách Khách hàng ĐÃ TỪNG MUA HÀNG và chưa bị xóa. Sử dụng EXISTS
     * để tối ưu tốc độ đọc trên Oracle.
     */
    public java.util.List<model.order.Customer> getCustomersWithOrders() {
        java.util.List<model.order.Customer> list = new java.util.ArrayList<>();

        // SQL CHUẨN THEO SCHEMA:
        // Lấy khách hàng chưa bị xóa (c.is_deleted = 0)
        // CÓ TỒN TẠI ít nhất 1 đơn hàng chưa bị xóa (o.is_deleted = 0)
        String sql = "SELECT customer_id, customer_name, phone, email, address "
                + "FROM CUSTOMERS c "
                + "WHERE c.is_deleted = 0 "
                + "AND EXISTS ("
                + "    SELECT 1 FROM ORDERS o "
                + "    WHERE o.customer_id = c.customer_id AND o.is_deleted = 0"
                + ")";

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.order.Customer c = new model.order.Customer();
                // Map chính xác tên cột trong schema
                c.setCustomerId(rs.getString("customer_id"));
                c.setCustomerName(rs.getString("customer_name"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setAddress(rs.getString("address"));

                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Customer> selectAllWithRank() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT c.*, "
                + "NVL((SELECT SUM(total_amount) FROM ORDERS WHERE customer_id = c.customer_id AND is_deleted = 0), 0) as total_spending "
                + "FROM CUSTOMERS c WHERE c.is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                Customer c = map(rs); // Hàm map cũ của ông
                double spending = rs.getDouble("total_spending");
                c.setTotalSpending(spending);

                // Logic xếp hạng nhanh trong Java (hoặc lấy từ SQL CASE ở Bước 1)
                if (spending >= 50000000) {
                    c.setMemberRank("Kim cương");
                } else if (spending >= 20000000) {
                    c.setMemberRank("Vàng");
                } else if (spending >= 5000000) {
                    c.setMemberRank("Bạc");
                } else {
                    c.setMemberRank("Đồng");
                }

                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
