package business.sql.rbac;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AccountAssignRoleSql {

    private static AccountAssignRoleSql instance;

    public static AccountAssignRoleSql getInstance() {
        if (instance == null) {
            instance = new AccountAssignRoleSql();
        }
        return instance;
    }

    public void assignDefaultRole(Connection conn, String accountId, String roleId) throws SQLException {
        String sql = "INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id, created_at) VALUES (?, ?, SYSDATE)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, accountId);
            pst.setString(2, roleId);
            pst.executeUpdate();
        }
    }
}
