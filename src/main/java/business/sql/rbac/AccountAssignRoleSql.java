package business.sql.rbac;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AccountAssignRoleSql {

    public static AccountAssignRoleSql getInstance() {
        return new AccountAssignRoleSql();
    }

    public int assignRoleToAccount(String accountId, String roleId) {
        return 0;
    }

    public int removeRoleFromAccount(String accountId, String roleId) {
        return 0;
    }

    // Trong file AccountAssignRoleSql.java, sửa lại hàm assignDefaultRole:
    public void assignDefaultRole(Connection conn, String accountId, String roleId) throws SQLException {
    // 1. Dùng ACCOUNT_ID thay cho username
    String sql = "INSERT INTO ACCOUNT_ASSIGN_ROLE (ACCOUNT_ID, ROLE_ID, CREATED_AT) VALUES (?, ?, SYSDATE)"; 
    
    try (PreparedStatement pst = conn.prepareStatement(sql)) {
        pst.setString(1, accountId);
        pst.setString(2, roleId);
        pst.executeUpdate();
    }
}
}
