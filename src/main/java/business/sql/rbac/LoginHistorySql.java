package business.sql.rbac;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import model.account.LoginHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoginHistorySql implements SqlInterface<LoginHistory> {

    private static final boolean DEBUG_LOG = Boolean.getBoolean("app.debug.loginHistory");

    public static LoginHistorySql getInstance() {
        return new LoginHistorySql();
    }

    @Override
    public int insert(LoginHistory h) {
        if (h == null) {
            return 0;
        }

        String resolvedAccountId = resolveAccountIdForHistory(h);

        if (resolvedAccountId == null || resolvedAccountId.trim().isEmpty()) {
            debug("[LoginHistory] Bo qua ghi log vi ACCOUNT_ID null. "
                    + "Action=" + h.getActionType()
                    + ", Status=" + h.getStatus()
                    + ", Reason=" + h.getFailureReason());
            return 0;
        }

        String sql
                = "INSERT INTO LOGIN_HISTORY ("
                + "LOG_ID, "
                + "ACCOUNT_ID, "
                + "ACTION_TYPE, "
                + "IP_ADDRESS, "
                + "DEVICE_INFO, "
                + "LOGIN_TIME, "
                + "STATUS, "
                + "FAILURE_REASON, "
                + "IS_DELETED"
                + ") VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, ?, ?, 0)";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            String logId = makeLogId(h.getLogId());

            pst.setString(1, logId);
            pst.setString(2, resolvedAccountId);
            pst.setString(3, fallback(h.getActionType(), "LOGIN"));
            pst.setString(4, fallback(h.getIpAddress(), "unknown"));
            pst.setString(5, fallback(h.getDeviceInfo(), "unknown"));
            pst.setString(6, fallback(h.getStatus(), "UNKNOWN"));
            pst.setString(7, h.getFailureReason());

            return pst.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[LoginHistory] Insert failed:");
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int update(LoginHistory h) {
        if (h == null || h.getLogId() == null || h.getLogId().trim().isEmpty()) {
            return 0;
        }

        String resolvedAccountId = resolveAccountIdForHistory(h);

        if (resolvedAccountId == null || resolvedAccountId.trim().isEmpty()) {
            debug("[LoginHistory] Bo qua update log vi ACCOUNT_ID null. LogId=" + h.getLogId());
            return 0;
        }

        String sql
                = "UPDATE LOGIN_HISTORY SET "
                + "ACCOUNT_ID = ?, "
                + "ACTION_TYPE = ?, "
                + "IP_ADDRESS = ?, "
                + "DEVICE_INFO = ?, "
                + "STATUS = ?, "
                + "FAILURE_REASON = ?, "
                + "IS_DELETED = ? "
                + "WHERE LOG_ID = ?";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, resolvedAccountId);
            pst.setString(2, fallback(h.getActionType(), "LOGIN"));
            pst.setString(3, fallback(h.getIpAddress(), "unknown"));
            pst.setString(4, fallback(h.getDeviceInfo(), "unknown"));
            pst.setString(5, fallback(h.getStatus(), "UNKNOWN"));
            pst.setString(6, h.getFailureReason());
            pst.setInt(7, h.getIsDeleted());
            pst.setString(8, h.getLogId().trim());

            return pst.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[LoginHistory] Update failed:");
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            return 0;
        }

        String sql = "UPDATE LOGIN_HISTORY SET IS_DELETED = 1 WHERE LOG_ID = ?";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id.trim());
            return pst.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[LoginHistory] Delete failed:");
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public LoginHistory selectById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM LOGIN_HISTORY WHERE LOG_ID = ? AND IS_DELETED = 0";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id.trim());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[LoginHistory] SelectById failed:");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ArrayList<LoginHistory> selectAll() {
        ArrayList<LoginHistory> list = new ArrayList<>();

        String sql
                = "SELECT * FROM LOGIN_HISTORY "
                + "WHERE IS_DELETED = 0 "
                + "ORDER BY LOGIN_TIME DESC";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LoginHistory] SelectAll failed:");
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public List<LoginHistory> selectByCondition(String condition) {
        ArrayList<LoginHistory> list = new ArrayList<>();

        if (condition == null || condition.trim().isEmpty()) {
            return list;
        }

        String sql = "SELECT * FROM LOGIN_HISTORY WHERE " + condition;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LoginHistory] SelectByCondition failed:");
            e.printStackTrace();
        }

        return list;
    }

    public ArrayList<LoginHistory> selectByAccountId(String accountId) {
        ArrayList<LoginHistory> list = new ArrayList<>();

        if (accountId == null || accountId.trim().isEmpty()) {
            return list;
        }

        String sql
                = "SELECT * FROM LOGIN_HISTORY "
                + "WHERE ACCOUNT_ID = ? "
                + "AND IS_DELETED = 0 "
                + "ORDER BY LOGIN_TIME DESC";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, accountId.trim());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[LoginHistory] SelectByAccountId failed:");
            e.printStackTrace();
        }

        return list;
    }

    public int softDeleteByAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return 0;
        }

        String sql = "UPDATE LOGIN_HISTORY SET IS_DELETED = 1 WHERE ACCOUNT_ID = ?";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, accountId.trim());
            return pst.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[LoginHistory] SoftDeleteByAccountId failed:");
            e.printStackTrace();
            return 0;
        }
    }

    public int log(String accountId,
            String actionType,
            String status,
            String failureReason,
            String ipAddress,
            String deviceInfo) {

        LoginHistory h = new LoginHistory();
        h.setAccountId(accountId);
        h.setActionType(actionType);
        h.setStatus(status);
        h.setFailureReason(failureReason);
        h.setIpAddress(ipAddress);
        h.setDeviceInfo(deviceInfo);

        return insert(h);
    }

    private LoginHistory map(ResultSet rs) throws SQLException {
        LoginHistory h = new LoginHistory();
        h.setLogId(rs.getString("LOG_ID"));
        h.setAccountId(rs.getString("ACCOUNT_ID"));
        h.setActionType(rs.getString("ACTION_TYPE"));
        h.setIpAddress(rs.getString("IP_ADDRESS"));
        h.setDeviceInfo(rs.getString("DEVICE_INFO"));
        h.setLoginTime(rs.getTimestamp("LOGIN_TIME"));
        h.setStatus(rs.getString("STATUS"));
        h.setFailureReason(rs.getString("FAILURE_REASON"));
        h.setIsDeleted(rs.getInt("IS_DELETED"));
        return h;
    }

    private String resolveAccountIdForHistory(LoginHistory h) {
        if (h == null) {
            return null;
        }

        String accountId = trimOrNull(h.getAccountId());

        if (accountId != null) {
            return accountId;
        }

        // Trường hợp login fail ACCOUNT_NOT_FOUND thì chưa có account_id.
        // Không cố resolve bằng username nữa vì LoginHistory model không có getUsername().
        // Trả null để insert() bỏ qua ghi log, tránh lỗi compile và tránh spam console.
        return null;
    }

    private String makeLogId(String input) {
        String id = trimOrNull(input);
        if (id != null) {
            return id;
        }

        return "LG" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String fallback(String value, String defaultValue) {
        String v = trimOrNull(value);
        return v == null ? defaultValue : v;
    }

    private String trimOrNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void debug(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }
}
