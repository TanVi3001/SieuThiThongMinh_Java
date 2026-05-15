package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.AuditLogSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;

public class AccountService {

    /**
     * Ghi audit log cho hành động đổi role user.
     */
    public static void logChangeRole(String targetAccountId, String oldRole, String newRole, String reason) {
        String actorId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getAccountId()
                : null;

        AuditLogSql.getInstance().log(
                actorId,
                "CHANGE_ROLE",
                "ACCOUNT",
                targetAccountId,
                "role=" + (oldRole != null ? oldRole : "UNKNOWN"),
                "role=" + (newRole != null ? newRole : "UNKNOWN"),
                reason != null ? reason : "Admin cap nhat quyen",
                localIp(),
                deviceInfo()
        );

        notifyAccountChanged("ROLE_CHANGED");
    }

    /**
     * Set trạng thái hoạt động của tài khoản. ONLINE = đang đăng nhập app.
     * OFFLINE = đã đăng xuất / không còn phiên làm việc.
     */
    public static boolean updateOnlineStatus(String accountId, String onlineStatus) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

        String status = "ONLINE".equalsIgnoreCase(onlineStatus) ? "ONLINE" : "OFFLINE";

        boolean ok = AccountSql.getInstance().updateOnlineStatus(accountId, status);

        if (ok) {
            notifyAccountChanged("ONLINE_STATUS_CHANGED");
            System.out.println("[AccountService] Account " + accountId + " -> " + status);
        }

        return ok;
    }

    public static boolean setOnline(String accountId) {
        return updateOnlineStatus(accountId, "ONLINE");
    }

    public static boolean setOffline(String accountId) {
        return updateOnlineStatus(accountId, "OFFLINE");
    }

    /**
     * Bắn realtime để các màn khác reload danh sách nhân viên/tài khoản.
     */
    private static void notifyAccountChanged(String message) {
        try {
            RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
            RealtimeClient.send("ACCOUNTS_CHANGED");
            RealtimeClient.send("EMPLOYEES_CHANGED");

            EventBus.publish(
                    new AppDataChangedEvent(
                            AppEventType.ACCOUNT_SECURITY,
                            message
                    )
            );

            System.out.println("[AccountService] Đã gửi realtime: " + message);

        } catch (Exception e) {
            System.err.println("[AccountService] Không thể gửi realtime: " + e.getMessage());
        }
    }

    private static String localIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String deviceInfo() {
        return System.getProperty("os.name") + " | Java " + System.getProperty("java.version");
    }

    public static void onLoginSuccess(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }

        boolean ok = AccountSql.getInstance().increaseActiveSession(accountId);

        if (ok) {
            notifyAccountStatusChanged("LOGIN_ONLINE");
        }
    }

    public static void onLogoutOrCloseApp(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }

        boolean ok = AccountSql.getInstance().decreaseActiveSession(accountId);

        if (ok) {
            notifyAccountStatusChanged("LOGOUT_OFFLINE_CHECK");
        }
    }

    public static void onLogoutOrCloseApp(String accountId, String sessionId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }

        model.account.Account currentUser
                = business.service.SessionManager.getCurrentUser();

        String role = getRoleOf(currentUser);

        boolean ok;

        if (isMultiSessionRole(role)) {
            ok = business.sql.rbac.AccountSql.getInstance()
                    .logoutMultiSession(accountId);
        } else {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return;
            }

            ok = business.sql.rbac.AccountSql.getInstance()
                    .logoutBySession(accountId, sessionId);
        }

        if (ok) {
            notifyOnlineStatusChanged("LOGOUT_BY_ROLE");
        }
    }

    private static void notifyOnlineStatusChanged(String message) {
        try {
            common.realtime.RealtimeClient.send("ACCOUNTS_CHANGED");
            common.realtime.RealtimeClient.send("EMPLOYEES_CHANGED");

            common.events.EventBus.publish(
                    new common.events.AppDataChangedEvent(
                            common.events.AppEventType.EMPLOYEES,
                            message
                    )
            );

        } catch (Exception e) {
            System.err.println("[AccountService] Realtime status error: " + e.getMessage());
        }
    }

    public static void sendHeartbeat(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }

        AccountSql.getInstance().heartbeat(accountId);
    }

    public static void cleanupDeadSessions() {
        int affected = business.sql.rbac.AccountSql.getInstance().resetDeadSessions();

        if (affected > 0) {
            notifyAccountStatusChanged("RESET_DEAD_SESSIONS");
        }
    }

    private static void notifyAccountStatusChanged(String message) {
        try {
            common.realtime.RealtimeClient.send("ACCOUNTS_CHANGED");
            common.realtime.RealtimeClient.send("EMPLOYEES_CHANGED");

            common.events.EventBus.publish(
                    new common.events.AppDataChangedEvent(
                            common.events.AppEventType.ACCOUNT_SECURITY,
                            message
                    )
            );

        } catch (Exception e) {
            System.err.println("[AccountService] Realtime error: " + e.getMessage());
        }
    }

    public static void onLoginSuccessForceSingleSession(String accountId, String sessionId) {
        if (accountId == null || accountId.trim().isEmpty()
                || sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        business.sql.rbac.AccountSql.getInstance()
                .activateSingleSession(accountId, sessionId);

        try {
            // Chỉ reload trạng thái tài khoản/nhân viên
            common.realtime.RealtimeClient.send("ACCOUNTS_CHANGED");
            common.realtime.RealtimeClient.send("EMPLOYEES_CHANGED");

            common.events.EventBus.publish(
                    new common.events.AppDataChangedEvent(
                            common.events.AppEventType.EMPLOYEES,
                            "FORCE_SINGLE_SESSION_LOGIN"
                    )
            );

        } catch (Exception ex) {
            System.err.println("[AccountService] Realtime error: " + ex.getMessage());
        }
    }

    public static boolean heartbeatAndCheckSession(String accountId, String sessionId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        model.account.Account currentUser
                = business.service.SessionManager.getCurrentUser();

        String role = getRoleOf(currentUser);

        if (isMultiSessionRole(role)) {
            // Admin / Manager không kiểm tra CURRENT_SESSION_ID
            // vì được phép mở nhiều app cùng tài khoản.
            return business.sql.rbac.AccountSql.getInstance()
                    .heartbeatMultiSession(accountId);
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        boolean updated = business.sql.rbac.AccountSql.getInstance()
                .heartbeatBySession(accountId, sessionId);

        if (!updated) {
            return false;
        }

        return business.sql.rbac.AccountSql.getInstance()
                .isCurrentSessionValid(accountId, sessionId);
    }

    private static boolean isMultiSessionRole(String role) {
        if (role == null) {
            return false;
        }

        return "R_ADMIN_ALL".equalsIgnoreCase(role)
                || "R_STORE_MNG".equalsIgnoreCase(role);
    }

    private static String getRoleOf(model.account.Account acc) {
        if (acc == null) {
            return "";
        }

        if (acc.getRoleId() != null && !acc.getRoleId().trim().isEmpty()) {
            return acc.getRoleId();
        }

        if (acc.getRoleValue() != null && !acc.getRoleValue().trim().isEmpty()) {
            return acc.getRoleValue();
        }

        if (acc.getRole() != null && !acc.getRole().trim().isEmpty()) {
            return acc.getRole();
        }

        return "";
    }

    public static void onLoginSuccessByRole(model.account.Account acc, String sessionId) {
        if (acc == null || acc.getAccountId() == null) {
            return;
        }

        String role = getRoleOf(acc);

        boolean ok;

        if (isMultiSessionRole(role)) {
            // Admin / Manager được đăng nhập song song
            ok = business.sql.rbac.AccountSql.getInstance()
                    .activateMultiSession(acc.getAccountId());
        } else {
            // Staff / Warehouse dùng Force Logout
            ok = business.sql.rbac.AccountSql.getInstance()
                    .activateSingleSession(acc.getAccountId(), sessionId);
        }

        if (ok) {
            notifyOnlineStatusChanged("LOGIN_BY_ROLE");
        }
    }
}
