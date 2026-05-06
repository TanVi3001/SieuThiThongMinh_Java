package business.sql.rbac;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.account.ActivationEmployeeInfo;

public class AccountActivationSql {

    private static AccountActivationSql instance;

    public static AccountActivationSql getInstance() {
        if (instance == null) {
            instance = new AccountActivationSql();
        }
        return instance;
    }

    public ActivationEmployeeInfo getEmployeeInfo(Connection con, String empId) throws SQLException {
        String sql = "SELECT employee_id, employee_name, phone, email FROM EMPLOYEES WHERE employee_id = ? AND NVL(is_deleted, 0) = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ActivationEmployeeInfo(rs.getString("employee_id"), rs.getString("employee_name"), rs.getString("phone"), rs.getString("email"));
                }
            }
        }
        return null;
    }

    public boolean existsAccountByUsername(Connection con, String username) throws SQLException {
        String sql = "SELECT 1 FROM ACCOUNTS WHERE username = ? AND NVL(is_deleted, 0) = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsAccountByUserId(Connection con, String userId) throws SQLException {
        String sql = "SELECT 1 FROM ACCOUNTS WHERE user_id = ? AND NVL(is_deleted, 0) = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // TẠO TÀI KHOẢN VÀ TRẢ VỀ ACCOUNT_ID (Ví dụ: ACC123456)
    public String insertAccount(Connection con, String userId, String username, String bcryptPasswordHash, String role) throws SQLException {
        String accountId = "ACC" + (System.currentTimeMillis() % 1000000000);
        String sql = "INSERT INTO ACCOUNTS (ACCOUNT_ID, USER_ID, USERNAME, PASSWORD, STATUS, CREATED_AT, IS_DELETED) VALUES (?, ?, ?, ?, ?, SYSDATE, 0)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.setString(2, userId);
            ps.setString(3, username);
            ps.setString(4, bcryptPasswordHash);
            ps.setString(5, "Hoạt động");
            int affected = ps.executeUpdate();
            return (affected == 1) ? accountId : null;
        }
    }

    public boolean existsUserById(Connection con, String userId) throws SQLException {
        String sql = "SELECT 1 FROM USERS WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertUser(Connection con, String userId, String fullName, String email, String phone) throws SQLException {
        String sql = "INSERT INTO USERS (user_id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.executeUpdate();
        }
    }

    public void updateEmployeeAccountStatus(Connection conn, String empId) throws SQLException {
//        String sql = "UPDATE EMPLOYEES SET account_status = N'Đã cấp' WHERE employee_id = ?";
//        try (PreparedStatement pst = conn.prepareStatement(sql)) {
//            pst.setString(1, empId);
//            pst.executeUpdate();
//        }
    }

    public String getEmployeeRole(Connection con, String empId) throws SQLException {
        String sql = "SELECT role_id FROM EMPLOYEES WHERE employee_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role_id");
                }
            }
        }
        return "R_STAFF_SALE"; // Trả về mặc định nếu có lỗi
    }
}
