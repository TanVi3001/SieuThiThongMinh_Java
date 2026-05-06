package common.sync;

import common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class SyncVersionDao {

    private SyncVersionDao() {
    }

    public static long getVersion(String syncKey) {
        String sql = "SELECT version_number FROM APP_SYNC WHERE sync_key = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, syncKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            System.err.println("SyncVersionDao.getVersion error: " + e.getMessage());
        }
        return -1;
    }

    public static void bumpVersion(String syncKey) {
        String sql = "UPDATE APP_SYNC SET version_number = version_number + 1, updated_at = CURRENT_TIMESTAMP WHERE sync_key = ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, syncKey);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("SyncVersionDao.bumpVersion error: " + e.getMessage());
        }
    }
}
