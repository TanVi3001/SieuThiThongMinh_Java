package business.sql.hr_kpi;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.employee.Employee;

public class EmployeeSql implements SqlInterface<Employee> {

    public static EmployeeSql getInstance() {
        return new EmployeeSql();
    }

    @Override
    public int insert(Employee t) {
        int res = 0;
        String sql = "INSERT INTO EMPLOYEES (employee_id, employee_name, phone, email, role_id, gender, store_id, hire_date, salary_coefficient, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, 1.0, 0)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            try {
                String roleId = (t.getRoleId() != null && !t.getRoleId().isEmpty()) ? t.getRoleId() : t.getRole();
                pst.setString(1, t.getEmployeeId());
                pst.setString(2, t.getEmployeeName());
                pst.setString(3, t.getPhone());
                pst.setString(4, t.getEmail());
                pst.setString(5, roleId);
                pst.setString(6, t.getGender());
                pst.setString(7, t.getStoreId());
                res = pst.executeUpdate();

                if (res > 0) {
                    try {
                        String newValue = "{employee_name:" + t.getEmployeeName() + ", phone:" + t.getPhone() + ", email:" + t.getEmail() + "}";
                        business.sql.rbac.AuditLogSql.logSystemEvent("CREATE", "EMPLOYEE", t.getEmployeeId(), null, newValue, "Tạo mới tài khoản Quản lý cửa hàng");
                    } catch (Exception e) {
                        System.err.println("Cảnh báo: Không thể ghi Audit Log cho chức năng tạo nhân viên.");
                    }
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                System.err.println("❌ LỖI KHI INSERT EMPLOYEE: " + e.getMessage());
                e.printStackTrace();
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    @Override
    public int update(Employee t) {
        String sql = "UPDATE EMPLOYEES SET employee_name = ?, phone = ?, email = ?, role_id = ?, gender = ?, store_id = ? "
                + "WHERE employee_id = ? AND is_deleted = 0";
        String sqlOld = "SELECT employee_name, phone, email, role_id, gender FROM EMPLOYEES WHERE employee_id = ? AND is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            try {
                String oldName = null, oldPhone = null, oldEmail = null, oldRole = null, oldGender = null;
                try (PreparedStatement psOld = con.prepareStatement(sqlOld)) {
                    psOld.setString(1, t.getEmployeeId());
                    try (ResultSet rs = psOld.executeQuery()) {
                        if (rs.next()) {
                            oldName = rs.getString("employee_name");
                            oldPhone = rs.getString("phone");
                            oldEmail = rs.getString("email");
                            oldRole = rs.getString("role_id");
                            oldGender = rs.getString("gender");
                        }
                    }
                }

                String newRole = t.getRoleId() != null ? t.getRoleId() : t.getRole();
                pst.setString(1, t.getEmployeeName());
                pst.setString(2, t.getPhone());
                pst.setString(3, t.getEmail());
                pst.setString(4, newRole);
                pst.setString(5, t.getGender());
                pst.setString(6, t.getStoreId());
                pst.setString(7, t.getEmployeeId());
                int rows = pst.executeUpdate();

                if (rows > 0) {
                    String oldValue = joinPairs(
                            diff(oldName, t.getEmployeeName()) ? pair("employee_name", oldName) : null,
                            diff(oldPhone, t.getPhone()) ? pair("phone", oldPhone) : null,
                            diff(oldEmail, t.getEmail()) ? pair("email", oldEmail) : null,
                            diff(oldRole, newRole) ? pair("role_id", oldRole) : null,
                            diff(oldGender, t.getGender()) ? pair("gender", oldGender) : null
                    );
                    String newValue = joinPairs(
                            diff(oldName, t.getEmployeeName()) ? pair("employee_name", t.getEmployeeName()) : null,
                            diff(oldPhone, t.getPhone()) ? pair("phone", t.getPhone()) : null,
                            diff(oldEmail, t.getEmail()) ? pair("email", t.getEmail()) : null,
                            diff(oldRole, newRole) ? pair("role_id", newRole) : null,
                            diff(oldGender, t.getGender()) ? pair("gender", t.getGender()) : null
                    );
                    if (newValue != null && !newValue.isBlank()) {
                        logAuditWithConn(con, "UPDATE_EMPLOYEE", "EMPLOYEE", t.getEmployeeId(), oldValue, newValue, "Cap nhat nhan vien");
                    }
                }
                con.commit();
                return rows;
            } catch (Exception e) {
                System.err.println("=== LỖI KHI CẬP NHẬT NHÂN VIÊN ===");
                e.printStackTrace();
                con.rollback();
                return 0;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        String sql = "UPDATE EMPLOYEES SET is_deleted = 1 WHERE employee_id = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            try {
                pst.setString(1, id);
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    logAuditWithConn(con, "DELETE_EMPLOYEE", "EMPLOYEE", id, pair("is_deleted", 0), pair("is_deleted", 1), "Xoa mem nhan vien");
                }
                con.commit();
                return rows;
            } catch (Exception e) {
                System.err.println("=== LỖI KHI XÓA NHÂN VIÊN ===");
                e.printStackTrace();
                con.rollback();
                return 0;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Employee selectById(String id) {
        String sql = "SELECT e.*, s.store_name FROM EMPLOYEES e LEFT JOIN STORES s ON e.store_id = s.store_id WHERE e.employee_id = ? AND e.is_deleted = 0";
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
    public List<Employee> selectAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, "
                + "NVL(e.role_id, N'Chưa phân bổ') AS actual_role, "
                + "NVL(a.status, N'Chưa cấp') AS account_status, "
                + "s.store_name AS store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(e.is_deleted, 0) = 0 "
                + "ORDER BY CASE WHEN e.role_id = 'R_ADMIN_ALL' THEN 1 WHEN e.role_id = 'R_STORE_MNG' THEN 2 ELSE 3 END, e.employee_name ASC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Employee emp = map(rs);
                emp.setRole(rs.getString("actual_role"));
                emp.setRoleId(rs.getString("actual_role"));
                emp.setAccountStatus(rs.getString("account_status"));
                list.add(emp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Employee> search(String keyword) {
        ArrayList<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, "
                + "NVL(e.role_id, N'Chưa phân bổ') AS actual_role, "
                + "NVL(a.status, N'Chưa cấp') AS account_status, "
                + "s.store_name AS store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(e.is_deleted, 0) = 0 "
                + "AND (LOWER(e.employee_name) LIKE LOWER(?) OR e.phone LIKE ? OR LOWER(e.email) LIKE LOWER(?) OR LOWER(NVL(s.store_name, '')) LIKE LOWER(?)) "
                + "ORDER BY e.employee_name ASC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String searchPattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee emp = map(rs);
                    emp.setRole(rs.getString("actual_role"));
                    emp.setRoleId(rs.getString("actual_role"));
                    emp.setAccountStatus(rs.getString("account_status"));
                    list.add(emp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Employee> searchByName(String name) {
        return search(name);
    }

    public int updateCompletedOrders(String employeeId, int count) {
        String sql = "UPDATE EMPLOYEES SET total_completed_orders = ? WHERE employee_id = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, count);
            pst.setString(2, employeeId);
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<Employee> selectByCondition(String condition) {
        ArrayList<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, NVL(e.role_id, N'Chưa phân bổ') AS actual_role, "
                + "NVL(a.status, N'Chưa cấp') AS account_status, s.store_name AS store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(e.is_deleted, 0) = 0 " + (condition == null ? "" : condition);
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Employee emp = map(rs);
                emp.setRole(rs.getString("actual_role"));
                emp.setRoleId(rs.getString("actual_role"));
                emp.setAccountStatus(rs.getString("account_status"));
                list.add(emp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getString("employee_id"));
        e.setEmployeeName(rs.getString("employee_name"));
        try { e.setHireDate(rs.getDate("hire_date")); } catch (SQLException ignored) {}
        try { e.setSalaryCoefficient(rs.getBigDecimal("salary_coefficient")); } catch (SQLException ignored) {}
        try { e.setTotalCompletedOrders(rs.getInt("total_completed_orders")); } catch (SQLException ignored) {}
        try { e.setShiftId(rs.getString("shift_id")); } catch (SQLException ignored) {}
        try { e.setIsDeleted(rs.getInt("is_deleted")); } catch (SQLException ignored) {}
        try { e.setPhone(rs.getString("phone")); } catch (SQLException ignored) {}
        try { e.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        try { e.setGender(rs.getString("gender")); } catch (SQLException ignored) {}
        try { e.setRoleId(rs.getString("role_id")); e.setRole(rs.getString("role_id")); } catch (SQLException ignored) {}
        try { e.setStoreId(rs.getString("store_id")); } catch (SQLException ignored) {}
        try { e.setStoreName(rs.getString("store_name")); } catch (SQLException ignored) {}
        return e;
    }

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
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(p);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void logAuditWithConn(Connection con, String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason) throws SQLException {
        model.account.AuditLog log = new model.account.AuditLog();
        log.setAccountId(business.service.SessionManager.getCurrentUser() != null ? business.service.SessionManager.getCurrentUser().getAccountId() : null);
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setIpAddress("local");
        log.setDeviceInfo(System.getProperty("os.name") + " | Java " + System.getProperty("java.version"));
        business.sql.rbac.AuditLogSql.getInstance().insertWithConn(con, log);
    }

    public boolean existsByEmailGlobal(String email, String excludeId) {
        String sql = "SELECT COUNT(*) FROM EMPLOYEES WHERE UPPER(email) = UPPER(?)";
        if (excludeId != null) {
            sql += " AND employee_id <> ?";
        }
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, email.trim());
            if (excludeId != null) pst.setString(2, excludeId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Employee> getAllNhanVien(String currentUserRole, String storeId) {
        List<Employee> list = new ArrayList<>();
        boolean isStoreManager = "R_STORE_MNG".equalsIgnoreCase(currentUserRole);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.employee_id, e.employee_name, e.phone, e.email, e.role_id, e.gender, e.store_id, ");
        sql.append("NVL(s.store_name, N'Chưa phân chi nhánh') AS store_name, ");
        sql.append("CASE WHEN a.account_id IS NOT NULL THEN N'Đã cấp' ELSE N'Chưa cấp' END AS account_status, ");
        sql.append("CASE WHEN a.account_id IS NULL THEN 'N/A' WHEN NVL(a.active_sessions, 0) > 0 THEN 'ONLINE' ELSE 'OFFLINE' END AS online_status, ");
        sql.append("NVL(a.active_sessions, 0) AS active_sessions ");
        sql.append("FROM EMPLOYEES e ");
        sql.append("LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 ");
        sql.append("LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 ");
        sql.append("WHERE NVL(e.is_deleted, 0) = 0 ");

        if (isStoreManager) {
            if (storeId == null || storeId.trim().isEmpty()) {
                System.err.println("Manager chưa có store_id, không load danh sách nhân viên.");
                return list;
            }
            sql.append("AND e.store_id = ? ");
            sql.append("AND NVL(e.role_id, 'UNKNOWN') <> 'R_ADMIN_ALL' ");
        }

        sql.append("ORDER BY CASE WHEN e.role_id = 'R_ADMIN_ALL' THEN 1 WHEN e.role_id = 'R_STORE_MNG' THEN 2 ELSE 3 END, e.employee_name ASC");

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (isStoreManager) ps.setString(1, storeId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee emp = new Employee();
                    emp.setEmployeeId(rs.getString("employee_id"));
                    emp.setEmployeeName(rs.getString("employee_name"));
                    emp.setPhone(rs.getString("phone"));
                    emp.setEmail(rs.getString("email"));
                    emp.setRoleId(rs.getString("role_id"));
                    emp.setRole(rs.getString("role_id"));
                    emp.setGender(rs.getString("gender"));
                    emp.setStoreId(rs.getString("store_id"));
                    emp.setStoreName(rs.getString("store_name"));
                    emp.setAccountStatus(rs.getString("account_status"));
                    emp.setOnlineStatus(rs.getString("online_status"));
                    emp.setActiveSessions(rs.getInt("active_sessions"));
                    list.add(emp);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public List<Employee> selectByStoreId(String storeId) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, NVL(e.role_id, N'Chưa phân bổ') AS actual_role, NVL(a.status, N'Chưa cấp') AS account_status, "
                + "CASE WHEN a.account_id IS NULL THEN 'N/A' WHEN NVL(a.active_sessions, 0) > 0 THEN 'ONLINE' ELSE 'OFFLINE' END AS online_status, "
                + "NVL(a.active_sessions, 0) AS active_sessions, s.store_name AS store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(e.is_deleted, 0) = 0 AND e.store_id = ? ORDER BY e.employee_name ASC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee emp = map(rs);
                    emp.setRole(rs.getString("actual_role"));
                    emp.setRoleId(rs.getString("actual_role"));
                    emp.setAccountStatus(rs.getString("account_status"));
                    emp.setOnlineStatus(rs.getString("online_status"));
                    emp.setActiveSessions(rs.getInt("active_sessions"));
                    list.add(emp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<Employee> searchByStoreId(String keyword, String storeId) {
        ArrayList<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, NVL(e.role_id, N'Chưa phân bổ') AS actual_role, NVL(a.status, N'Chưa cấp') AS account_status, "
                + "CASE WHEN a.account_id IS NULL THEN 'N/A' WHEN NVL(a.active_sessions, 0) > 0 THEN 'ONLINE' ELSE 'OFFLINE' END AS online_status, "
                + "NVL(a.active_sessions, 0) AS active_sessions, s.store_name AS store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN ACCOUNTS a ON e.employee_id = a.user_id AND NVL(a.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(e.is_deleted, 0) = 0 AND e.store_id = ? "
                + "AND (LOWER(e.employee_name) LIKE LOWER(?) OR e.phone LIKE ? OR LOWER(e.email) LIKE LOWER(?)) "
                + "ORDER BY e.employee_name ASC";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, storeId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee emp = map(rs);
                    emp.setRole(rs.getString("actual_role"));
                    emp.setRoleId(rs.getString("actual_role"));
                    emp.setAccountStatus(rs.getString("account_status"));
                    emp.setOnlineStatus(rs.getString("online_status"));
                    emp.setActiveSessions(rs.getInt("active_sessions"));
                    list.add(emp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int updateInStore(Employee t, String storeId) {
        String sql = "UPDATE EMPLOYEES SET employee_name = ?, phone = ?, email = ?, role_id = ?, gender = ?, store_id = ? "
                + "WHERE employee_id = ? AND store_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            String roleId = (t.getRoleId() != null && !t.getRoleId().isEmpty()) ? t.getRoleId() : t.getRole();
            pst.setString(1, t.getEmployeeName());
            pst.setString(2, t.getPhone());
            pst.setString(3, t.getEmail());
            pst.setString(4, roleId);
            pst.setString(5, t.getGender());
            pst.setString(6, storeId);
            pst.setString(7, t.getEmployeeId());
            pst.setString(8, storeId);
            return pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int deleteInStore(String employeeId, String storeId) {
        String sql = "UPDATE EMPLOYEES SET is_deleted = 1 WHERE employee_id = ? AND store_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, employeeId);
            pst.setString(2, storeId);
            return pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
