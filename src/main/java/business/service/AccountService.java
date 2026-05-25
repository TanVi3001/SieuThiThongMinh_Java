package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.AuditLogSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;

public class AccountService {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.account");

    private AccountService() {
    }

    public static void logChangeRole(String targetAccountId, String oldRole, String newRole, String reason) {
        String actorId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getAccountId()
                : null;

        AuditLogSql.getInstance().log(
                actorId,
                "CHANGE_ROLE",
                "ACCOUNT",
                targetAccountId,
                "role=" + safe(oldRole, "UNKNOWN"),
                "role=" + safe(newRole, "UNKNOWN"),
                reason != null ? reason : "Cap nhat quyen tai khoan",
                localIp(),
                deviceInfo()
        );

        notifyAccountSecurityChanged("ROLE_CHANGED");
    }

    public static boolean updateOnlineStatus(String accountId, String onlineStatus) {
        accountId = clean(accountId);

        if (accountId == null) {
            return false;
        }

        String status = "ONLINE".equalsIgnoreCase(onlineStatus) ? "ONLINE" : "OFFLINE";

        boolean ok = AccountSql.getInstance().updateOnlineStatus(accountId, status);

        if (ok) {
            notifyOnlineStatusChanged("ONLINE_STATUS_CHANGED");
            debug("[AccountService] Account " + accountId + " -> " + status);
        }

        return ok;
    }

    public static boolean setOnline(String accountId) {
        return updateOnlineStatus(accountId, "ONLINE");
    }

    public static boolean setOffline(String accountId) {
        return updateOnlineStatus(accountId, "OFFLINE");
    }

    public static void onLoginSuccess(String accountId, String sessionId) {
        accountId = clean(accountId);
        sessionId = clean(sessionId);

        if (accountId == null || sessionId == null) {
            return;
        }

        try {
            AccountSql accountSql = AccountSql.getInstance();

            accountSql.createLoginSession(accountId, sessionId);
            accountSql.heartbeatSession(accountId, sessionId);
            accountSql.heartbeat(accountId);
            accountSql.cleanupDeadSessions();

            notifyOnlineStatusChanged("LOGIN_ONLINE");

        } catch (Exception e) {
            System.err.println("[AccountService] onLoginSuccess error: " + e.getMessage());
        }
    }

    public static void onLoginSuccess(String accountId) {
        String sessionId = SessionManager.getCurrentSessionId();
        onLoginSuccess(accountId, sessionId);
    }

    public static void onLogoutOrCloseApp(String accountId, String sessionId) {
        accountId = clean(accountId);
        sessionId = clean(sessionId);

        if (accountId == null) {
            return;
        }

        try {
            AccountSql accountSql = AccountSql.getInstance();

            if (sessionId != null) {
                accountSql.closeLoginSession(accountId, sessionId);
            }

            try {
                accountSql.decreaseActiveSession(accountId);
            } catch (Exception ignored) {
            }

            accountSql.cleanupDeadSessions();
            notifyOnlineStatusChanged("LOGOUT_OR_CLOSE_APP");

        } catch (Exception e) {
            System.err.println("[AccountService] onLogoutOrCloseApp error: " + e.getMessage());
        }
    }

    public static void onLogoutOrCloseApp(String accountId) {
        String sessionId = SessionManager.getCurrentSessionId();
        onLogoutOrCloseApp(accountId, sessionId);
    }

    public static void sendHeartbeat(String accountId) {
        accountId = clean(accountId);

        if (accountId == null) {
            return;
        }

        try {
            String sessionId = SessionManager.getCurrentSessionId();
            AccountSql accountSql = AccountSql.getInstance();

            if (sessionId != null && !sessionId.trim().isEmpty()) {
                boolean updated = accountSql.heartbeatSession(accountId, sessionId);

                if (!updated) {
                    accountSql.createLoginSession(accountId, sessionId);
                    accountSql.heartbeatSession(accountId, sessionId);
                }
            }

            accountSql.heartbeat(accountId);
            accountSql.cleanupDeadSessions();

        } catch (Exception e) {
            System.err.println("[AccountService] sendHeartbeat error: " + e.getMessage());
        }
    }

    public static void cleanupDeadSessions() {
        try {
            AccountSql.getInstance().cleanupDeadSessions();
            notifyOnlineStatusChanged("CLEANUP_DEAD_SESSIONS");
        } catch (Exception e) {
            System.err.println("[AccountService] cleanupDeadSessions error: " + e.getMessage());
        }
    }

    public static void onLoginSuccessForceSingleSession(String accountId, String sessionId) {
        onLoginSuccess(accountId, sessionId);
    }

    public static void onLoginSuccessByRole(model.account.Account acc, String sessionId) {
        if (acc == null || acc.getAccountId() == null) {
            return;
        }

        onLoginSuccess(acc.getAccountId(), sessionId);
    }

    public static boolean heartbeatAndCheckSession(String accountId, String sessionId) {
        accountId = clean(accountId);
        sessionId = clean(sessionId);

        if (accountId == null || sessionId == null) {
            return false;
        }

        try {
            AccountSql accountSql = AccountSql.getInstance();

            accountSql.cleanupDeadSessions();

            boolean updated = accountSql.heartbeatSession(accountId, sessionId);

            if (!updated) {
                accountSql.createLoginSession(accountId, sessionId);
                updated = accountSql.heartbeatSession(accountId, sessionId);
            }

            accountSql.heartbeat(accountId);

            return updated;

        } catch (Exception e) {
            System.err.println("[AccountService] heartbeatAndCheckSession error: " + e.getMessage());
            return false;
        }
    }

    public static void notifyAccountSecurityChanged(String message) {
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

            debug("[AccountService] realtime security: " + message);

        } catch (Exception e) {
            System.err.println("[AccountService] Không thể gửi realtime security: " + e.getMessage());
        }
    }

    private static void notifyOnlineStatusChanged(String message) {
        try {
            RealtimeClient.send("ACCOUNTS_CHANGED");
            RealtimeClient.send("EMPLOYEES_CHANGED");

            EventBus.publish(
                    new AppDataChangedEvent(
                            AppEventType.EMPLOYEES,
                            message
                    )
            );

            debug("[AccountService] realtime online: " + message);

        } catch (Exception e) {
            System.err.println("[AccountService] Realtime online error: " + e.getMessage());
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

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String safe(String value, String fallback) {
        value = clean(value);
        return value == null ? fallback : value;
    }

    private static void debug(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }
}
