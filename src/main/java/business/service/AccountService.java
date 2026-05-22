package business.service;

import business.sql.rbac.AccountSql;
import business.sql.rbac.AuditLogSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;

public class AccountService {

    private AccountService() {
    }

    /**
     * Ghi audit log cho hành động đổi role user. Sau khi đổi role phải bắn
     * ACCOUNT_SECURITY_CHANGED để máy user đang online tự kiểm tra quyền và
     * logout nếu role bị đổi.
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
                "role=" + safe(oldRole, "UNKNOWN"),
                "role=" + safe(newRole, "UNKNOWN"),
                reason != null ? reason : "Cap nhat quyen tai khoan",
                localIp(),
                deviceInfo()
        );

        notifyAccountSecurityChanged("ROLE_CHANGED");
    }

    /**
     * Set trạng thái ONLINE/OFFLINE ở bảng ACCOUNTS. Lưu ý: trạng thái hiển thị
     * số phiên online thật nên lấy từ ACCOUNT_SESSIONS. Hàm này chỉ giữ tương
     * thích cho các màn cũ còn đọc ONLINE_STATUS.
     */
    public static boolean updateOnlineStatus(String accountId, String onlineStatus) {
        accountId = clean(accountId);

        if (accountId == null) {
            return false;
        }

        String status = "ONLINE".equalsIgnoreCase(onlineStatus) ? "ONLINE" : "OFFLINE";

        boolean ok = AccountSql.getInstance().updateOnlineStatus(accountId, status);

        if (ok) {
            notifyOnlineStatusChanged("ONLINE_STATUS_CHANGED");
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
     * Login thành công theo logic mới: - Tạo/cập nhật một dòng trong
     * ACCOUNT_SESSIONS. - Mỗi cửa sổ app có một sessionId riêng. - Không kick
     * phiên cũ khi Manager/Admin đăng nhập nhiều thiết bị.
     */
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

    /**
     * Giữ hàm cũ để code cũ gọi không bị lỗi. Nếu không truyền sessionId thì
     * lấy session hiện tại từ SessionManager.
     */
    public static void onLoginSuccess(String accountId) {
        String sessionId = SessionManager.getCurrentSessionId();
        onLoginSuccess(accountId, sessionId);
    }

    /**
     * Logout hoặc đóng app theo logic mới: - Đóng đúng session hiện tại trong
     * ACCOUNT_SESSIONS. - Không làm sai số phiên còn lại nếu tài khoản đang mở
     * nhiều cửa sổ.
     */
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

            /*
             * Giữ lại để các màn cũ còn dùng ACTIVE_SESSIONS không bị quá bẩn.
             * Nhưng UI online chuẩn nên đọc từ ACCOUNT_SESSIONS.
             */
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

    /**
     * Giữ hàm cũ để code cũ gọi không bị lỗi. Sửa bug cũ: trước đây hàm này
     * dùng biến sessionId không tồn tại.
     */
    public static void onLogoutOrCloseApp(String accountId) {
        String sessionId = SessionManager.getCurrentSessionId();
        onLogoutOrCloseApp(accountId, sessionId);
    }

    /**
     * Heartbeat cho session hiện tại. Dùng nếu có file cũ gọi
     * AccountService.sendHeartbeat(accountId).
     */
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

    /**
     * Dọn session chết. Logic mới không dùng resetDeadSessions() nữa.
     */
    public static void cleanupDeadSessions() {
        try {
            AccountSql.getInstance().cleanupDeadSessions();
            notifyOnlineStatusChanged("CLEANUP_DEAD_SESSIONS");
        } catch (Exception e) {
            System.err.println("[AccountService] cleanupDeadSessions error: " + e.getMessage());
        }
    }

    /**
     * Legacy alias. Trước đây dùng force single session. Bây giờ chuyển sang
     * tạo session bình thường để không đá Manager/Admin.
     */
    public static void onLoginSuccessForceSingleSession(String accountId, String sessionId) {
        onLoginSuccess(accountId, sessionId);
    }

    /**
     * Legacy alias. Trước đây phân biệt single/multi theo role. Bây giờ mọi
     * role đều ghi ACCOUNT_SESSIONS. Việc staff có bị kick khi đổi role/quyền
     * sẽ do SecurityGuard xử lý.
     */
    public static void onLoginSuccessByRole(model.account.Account acc, String sessionId) {
        if (acc == null || acc.getAccountId() == null) {
            return;
        }

        onLoginSuccess(acc.getAccountId(), sessionId);
    }

    /**
     * Legacy method cho HeartbeatService cũ. Nếu còn file nào chưa sửa vẫn gọi
     * hàm này thì vẫn chạy được.
     *
     * Logic mới: - Không check CURRENT_SESSION_ID. - Không kick phiên cũ khi
     * login thiết bị khác. - Chỉ cập nhật heartbeat cho ACCOUNT_SESSIONS.
     */
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

    /**
     * Bắn realtime khi đổi role/quyền/bảo mật tài khoản. SecurityGuard sẽ bắt
     * ACCOUNT_SECURITY_CHANGED và tự kiểm tra role hiện tại.
     */
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

            System.out.println("[AccountService] Đã gửi realtime security: " + message);

        } catch (Exception e) {
            System.err.println("[AccountService] Không thể gửi realtime security: " + e.getMessage());
        }
    }

    /**
     * Bắn realtime khi trạng thái online/offline thay đổi.
     */
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

            System.out.println("[AccountService] Đã gửi realtime online: " + message);

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
}
