package business.service;

import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.account.Account;

public class SessionScopeService {

    private SessionScopeService() {
    }

    public static void loadEmployeeStoreScope(Account account) {
        if (account == null || account.getUserId() == null || account.getUserId().isBlank()) {
            return;
        }

        String sql
                = "SELECT e.employee_id, e.store_id, s.store_name "
                + "FROM EMPLOYEES e "
                + "LEFT JOIN STORES s ON e.store_id = s.store_id "
                + "WHERE e.employee_id = ? AND NVL(e.is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, account.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SessionManager.setCurrentEmployeeScope(
                            rs.getString("employee_id"),
                            rs.getString("store_id"),
                            rs.getString("store_name")
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi load store scope cho user đăng nhập: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
