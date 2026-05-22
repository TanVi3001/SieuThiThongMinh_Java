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

        String role = clean(readRole(account));
        String userId = clean(account.getUserId());
        String accountId = clean(account.getAccountId());
        String username = clean(account.getUsername());

        // Admin là tài khoản toàn hệ thống.
        // Không nên gắn store_id cho Admin để tránh các màn hiểu nhầm Admin bị scope ST001.
        if ("R_ADMIN_ALL".equalsIgnoreCase(role)) {
            SessionManager.setCurrentEmployeeScope(userId, null, "Toàn hệ thống");
            System.out.println("[SESSION_SCOPE] admin global scope: "
                    + "accountId=" + accountId
                    + ", userId=" + userId
                    + ", username=" + username
                    + ", role=" + role);
            return;
        }

        /*
         * Logic load scope:
         *
         * Ưu tiên 1:
         *   ACCOUNTS.user_id = EMPLOYEES.employee_id
         *
         * Fallback 2:
         *   Nếu ACCOUNTS.user_id là USERS.user_id dạng USR...
         *   thì dò EMPLOYEES theo email trong USERS.
         *
         * Fallback 3:
         *   Dò EMPLOYEES theo phone trong USERS.
         *
         * Mục tiêu:
         *   Chống lỗi dữ liệu cũ tạo account user_id = USR...
         *   nhưng employee thật có store_id.
         */
        String sql = ""
                + "SELECT "
                + "    a.account_id, "
                + "    a.user_id, "
                + "    a.username, "
                + "    COALESCE(e_direct.employee_id, e_email.employee_id, e_phone.employee_id) AS employee_id, "
                + "    COALESCE(e_direct.store_id, e_email.store_id, e_phone.store_id) AS store_id, "
                + "    s.store_name AS store_name, "
                + "    CASE "
                + "        WHEN e_direct.employee_id IS NOT NULL THEN 'DIRECT_USER_ID' "
                + "        WHEN e_email.employee_id IS NOT NULL THEN 'FALLBACK_EMAIL' "
                + "        WHEN e_phone.employee_id IS NOT NULL THEN 'FALLBACK_PHONE' "
                + "        ELSE 'NOT_FOUND' "
                + "    END AS match_type "
                + "FROM ACCOUNTS a "
                + "LEFT JOIN USERS u "
                + "       ON u.user_id = a.user_id "
                + "      AND NVL(u.is_deleted, 0) = 0 "
                + "LEFT JOIN EMPLOYEES e_direct "
                + "       ON e_direct.employee_id = a.user_id "
                + "      AND NVL(e_direct.is_deleted, 0) = 0 "
                + "LEFT JOIN EMPLOYEES e_email "
                + "       ON e_direct.employee_id IS NULL "
                + "      AND u.email IS NOT NULL "
                + "      AND LOWER(TRIM(e_email.email)) = LOWER(TRIM(u.email)) "
                + "      AND NVL(e_email.is_deleted, 0) = 0 "
                + "LEFT JOIN EMPLOYEES e_phone "
                + "       ON e_direct.employee_id IS NULL "
                + "      AND e_email.employee_id IS NULL "
                + "      AND u.phone_number IS NOT NULL "
                + "      AND REGEXP_REPLACE(NVL(e_phone.phone, ''), '[^0-9]', '') = REGEXP_REPLACE(NVL(u.phone_number, ''), '[^0-9]', '') "
                + "      AND NVL(e_phone.is_deleted, 0) = 0 "
                + "LEFT JOIN STORES s "
                + "       ON s.store_id = COALESCE(e_direct.store_id, e_email.store_id, e_phone.store_id) "
                + "      AND NVL(s.is_deleted, 0) = 0 "
                + "WHERE NVL(a.is_deleted, 0) = 0 "
                + "  AND ( "
                + "        (? IS NOT NULL AND a.user_id = ?) "
                + "     OR (? IS NOT NULL AND a.account_id = ?) "
                + "     OR (? IS NOT NULL AND a.username = ?) "
                + "  ) "
                + "ORDER BY "
                + "    CASE "
                + "        WHEN e_direct.employee_id IS NOT NULL THEN 1 "
                + "        WHEN e_email.employee_id IS NOT NULL THEN 2 "
                + "        WHEN e_phone.employee_id IS NOT NULL THEN 3 "
                + "        ELSE 9 "
                + "    END, "
                + "    COALESCE(e_direct.employee_id, e_email.employee_id, e_phone.employee_id) "
                + "FETCH FIRST 1 ROWS ONLY";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
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
                    String matchType = clean(rs.getString("match_type"));

                    /*
                     * Nếu query chưa tìm được employee_id nhưng Account.user_id có dạng MNG/EMP/ADM
                     * thì vẫn giữ lại employeeId fallback để các màn không bị null.
                     * Tuy nhiên storeId vẫn phải lấy từ EMPLOYEES, không gán cứng.
                     */
                    if (employeeId == null && looksLikeEmployeeId(userId)) {
                        employeeId = userId;
                    }

                    SessionManager.setCurrentEmployeeScope(employeeId, storeId, storeName);

                    System.out.println("[SESSION_SCOPE] loaded: "
                            + "accountId=" + accountId
                            + ", userId=" + userId
                            + ", username=" + username
                            + ", role=" + role
                            + ", employeeId=" + employeeId
                            + ", storeId=" + storeId
                            + ", storeName=" + storeName
                            + ", matchType=" + matchType);
                } else {
                    // Fallback cuối: nếu userId nhìn giống employeeId thì set employeeId,
                    // nhưng không set storeId để tránh gán sai chi nhánh.
                    String fallbackEmployeeId = looksLikeEmployeeId(userId) ? userId : null;

                    SessionManager.setCurrentEmployeeScope(fallbackEmployeeId, null, null);

                    System.out.println("[SESSION_SCOPE] not found: "
                            + "accountId=" + accountId
                            + ", userId=" + userId
                            + ", username=" + username
                            + ", role=" + role
                            + ", fallbackEmployeeId=" + fallbackEmployeeId);
                }
            }

        } catch (Exception e) {
            System.err.println("[SESSION_SCOPE] Lỗi load store scope: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String readRole(Account account) {
        if (account == null) {
            return null;
        }

        try {
            String role = account.getRole();
            if (role != null && !role.trim().isEmpty()) {
                return role.trim();
            }
        } catch (Exception ignored) {
        }

        try {
            String roleId = account.getRoleId();
            if (roleId != null && !roleId.trim().isEmpty()) {
                return roleId.trim();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static boolean looksLikeEmployeeId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String v = value.trim().toUpperCase();

        return v.startsWith("EMP")
                || v.startsWith("MNG")
                || v.startsWith("ADM")
                || v.startsWith("STAFF");
    }

    private static String clean(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
