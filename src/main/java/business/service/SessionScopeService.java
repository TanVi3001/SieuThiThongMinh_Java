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
        SessionManager.setCurrentEmployeeScope(null, null, null);

        if (account == null) {
            System.out.println("[SESSION_SCOPE] account=null");
            return;
        }

        String userId = clean(account.getUserId());
        String accountId = clean(account.getAccountId());
        String username = clean(account.getUsername());

        String sql = ""
                + "SELECT e.employee_id, e.store_id, s.store_name "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN EMPLOYEES e "
                + "       ON e.employee_id = a.user_id "
                + "      AND NVL(e.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s "
                + "       ON s.store_id = e.store_id "
                + "      AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(a.is_deleted, 0) = 0 "
                + "  AND ( "
                + "        (? IS NOT NULL AND a.user_id = ?) "
                + "     OR (? IS NOT NULL AND a.account_id = ?) "
                + "     OR (? IS NOT NULL AND a.username = ?) "
                + "  )";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, userId);

            ps.setString(3, accountId);
            ps.setString(4, accountId);

            ps.setString(5, username);
            ps.setString(6, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String employeeId = clean(rs.getString("employee_id"));
                    String storeId = clean(rs.getString("store_id"));
                    String storeName = clean(rs.getString("store_name"));

                    SessionManager.setCurrentEmployeeScope(employeeId, storeId, storeName);

                    System.out.println("[SESSION_SCOPE] loaded: "
                            + "accountId=" + accountId
                            + ", userId=" + userId
                            + ", username=" + username
                            + ", employeeId=" + employeeId
                            + ", storeId=" + storeId
                            + ", storeName=" + storeName);
                } else {
                    System.out.println("[SESSION_SCOPE] not found: "
                            + "accountId=" + accountId
                            + ", userId=" + userId
                            + ", username=" + username);
                }
            }

        } catch (Exception e) {
            System.err.println("[SESSION_SCOPE] Lỗi load store scope: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String clean(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
