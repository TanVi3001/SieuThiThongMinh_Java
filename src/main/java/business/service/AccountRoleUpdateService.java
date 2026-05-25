package business.service;

import common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Safe service for changing one account's direct role.
 *
 * Why this exists:
 * - Old ACCOUNT_ASSIGN_ROLE data can contain inactive history rows.
 * - Updating one arbitrary ROWID to a target role can hit ORA-00001
 *   when the same account_id + role_id already exists in another row.
 *
 * Strategy:
 * - Soft-delete role groups and all direct roles for the account.
 * - Restore existing target direct role if it exists.
 * - Insert target direct role only when it does not exist.
 * - Sync EMPLOYEES.role_id for screens that read employee role directly.
 */
public final class AccountRoleUpdateService {

    private AccountRoleUpdateService() {
    }

    public static boolean updateAccountRoleSafely(String accountId, String newRoleId) {
        String cleanAccountId = clean(accountId);
        String cleanRoleId = clean(newRoleId);

        if (cleanAccountId == null || cleanRoleId == null) {
            return false;
        }

        cleanRoleId = cleanRoleId.toUpperCase();

        String sqlGetUserId = """
            SELECT user_id
            FROM ACCOUNTS
            WHERE account_id = ?
              AND NVL(is_deleted, 0) = 0
            FETCH FIRST 1 ROWS ONLY
        """;

        String sqlDisableRoleGroups = """
            UPDATE ACCOUNT_ASSIGN_ROLE_GROUP
            SET is_deleted = 1
            WHERE account_id = ?
        """;

        String sqlDisableDirectRoles = """
            UPDATE ACCOUNT_ASSIGN_ROLE
            SET is_deleted = 1
            WHERE account_id = ?
        """;

        String sqlRestoreTargetRole = """
            UPDATE ACCOUNT_ASSIGN_ROLE
            SET is_deleted = 0
            WHERE account_id = ?
              AND role_id = ?
        """;

        String sqlInsertTargetRole = """
            INSERT INTO ACCOUNT_ASSIGN_ROLE (account_id, role_id, is_deleted)
            SELECT ?, ?, 0
            FROM dual
            WHERE NOT EXISTS (
                SELECT 1
                FROM ACCOUNT_ASSIGN_ROLE
                WHERE account_id = ?
                  AND role_id = ?
            )
        """;

        String sqlUpdateAccountRoleColumn = """
            UPDATE ACCOUNTS
            SET role = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlUpdateEmployeeRole = """
            UPDATE EMPLOYEES
            SET role_id = ?
            WHERE employee_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        Connection con = null;
        boolean oldAutoCommit = true;

        try {
            con = DatabaseConnection.getConnection();
            if (con == null) {
                return false;
            }

            oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            String userId = null;
            try (PreparedStatement ps = con.prepareStatement(sqlGetUserId)) {
                ps.setString(1, cleanAccountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = clean(rs.getString("user_id"));
                    }
                }
            }

            if (userId == null) {
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDisableRoleGroups)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDisableDirectRoles)) {
                ps.setString(1, cleanAccountId);
                ps.executeUpdate();
            }

            int restored;
            try (PreparedStatement ps = con.prepareStatement(sqlRestoreTargetRole)) {
                ps.setString(1, cleanAccountId);
                ps.setString(2, cleanRoleId);
                restored = ps.executeUpdate();
            }

            if (restored <= 0) {
                try (PreparedStatement ps = con.prepareStatement(sqlInsertTargetRole)) {
                    ps.setString(1, cleanAccountId);
                    ps.setString(2, cleanRoleId);
                    ps.setString(3, cleanAccountId);
                    ps.setString(4, cleanRoleId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateAccountRoleColumn)) {
                ps.setString(1, cleanRoleId);
                ps.setString(2, cleanAccountId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sqlUpdateEmployeeRole)) {
                ps.setString(1, cleanRoleId);
                ps.setString(2, userId);
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }
            System.err.println("[AccountRoleUpdateService] update role error: " + e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (Exception ignored) {
                }
                try {
                    con.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
