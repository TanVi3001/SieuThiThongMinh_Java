package business.sql.prod_inventory;

import business.sql.SqlInterface;
import common.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.product.Inventory;

public class InventorySql implements SqlInterface<Inventory> {

    private static InventorySql instance;

    private InventorySql() {
    }

    public static InventorySql getInstance() {
        if (instance == null) {
            instance = new InventorySql();
        }
        return instance;
    }

    @Override
    public int insert(Inventory t) {
        if (t == null) {
            return 0;
        }

        String sql = """
            INSERT INTO INVENTORY (
                product_id,
                store_id,
                quantity,
                unit,
                last_updated,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql)
        ) {
            pst.setString(1, t.getProductId());
            pst.setString(2, t.getStoreId());
            pst.setInt(3, t.getQuantity());
            pst.setString(4, t.getUnit());
            pst.setDate(5, t.getLastUpdated());
            pst.setInt(6, t.getIsDeleted());

            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public ArrayList<Inventory> selectAll() {
        ArrayList<Inventory> result = new ArrayList<>();

        String sql = """
            SELECT product_id,
                   store_id,
                   quantity,
                   unit,
                   last_updated,
                   is_deleted
            FROM INVENTORY
            WHERE NVL(is_deleted, 0) = 0
            ORDER BY store_id, product_id
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql);
                ResultSet rs = pst.executeQuery()
        ) {
            while (rs.next()) {
                result.add(mapInventory(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public ArrayList<Inventory> selectAllByStore(String storeId) {
        ArrayList<Inventory> result = new ArrayList<>();

        if (storeId == null || storeId.trim().isEmpty()) {
            return result;
        }

        String sql = """
            SELECT product_id,
                   store_id,
                   quantity,
                   unit,
                   last_updated,
                   is_deleted
            FROM INVENTORY
            WHERE TRIM(store_id) = TRIM(?)
              AND NVL(is_deleted, 0) = 0
            ORDER BY product_id
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql)
        ) {
            pst.setString(1, storeId.trim());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(mapInventory(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public int update(Inventory t) {
        if (t == null) {
            return 0;
        }

        String sql = """
            UPDATE INVENTORY
            SET quantity = ?,
                unit = ?,
                last_updated = ?,
                is_deleted = ?
            WHERE product_id = ?
              AND store_id = ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql)
        ) {
            pst.setInt(1, t.getQuantity());
            pst.setString(2, t.getUnit());
            pst.setDate(3, t.getLastUpdated());
            pst.setInt(4, t.getIsDeleted());
            pst.setString(5, t.getProductId());
            pst.setString(6, t.getStoreId());

            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(String id) {
        return 0;
    }

    @Override
    public Inventory selectById(String id) {
        return null;
    }

    public int deleteByCompositeKey(String productId, String storeId) {
        if (productId == null || productId.trim().isEmpty()
                || storeId == null || storeId.trim().isEmpty()) {
            return 0;
        }

        String sql = """
            UPDATE INVENTORY
            SET is_deleted = 1,
                last_updated = CURRENT_TIMESTAMP
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql)
        ) {
            pst.setString(1, productId.trim());
            pst.setString(2, storeId.trim());

            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public Inventory selectByCompositeKey(String productId, String storeId) {
        if (productId == null || productId.trim().isEmpty()
                || storeId == null || storeId.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT product_id,
                   store_id,
                   quantity,
                   unit,
                   last_updated,
                   is_deleted
            FROM INVENTORY
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(sql)
        ) {
            pst.setString(1, productId.trim());
            pst.setString(2, storeId.trim());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapInventory(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Inventory> selectByCondition(String condition) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Hàm cũ giữ lại để tránh code cũ compile lỗi.
     * Không nên dùng nữa vì không có store_id.
     */
    public int subtractStock(Connection con, String productId, int quantity) throws SQLException {
        throw new SQLException(
                "subtractStock(productId, quantity) không an toàn vì thiếu store_id. "
                + "Hãy dùng subtractStock(con, productId, quantity, storeId)."
        );
    }

    public int subtractStock(Connection con, String productId, int quantity, String storeId) throws SQLException {
        if (productId == null || productId.trim().isEmpty()) {
            throw new SQLException("Mã sản phẩm không hợp lệ.");
        }

        if (storeId == null || storeId.trim().isEmpty()) {
            throw new SQLException("Thiếu store_id khi trừ tồn kho.");
        }

        if (quantity <= 0) {
            throw new SQLException("Số lượng trừ kho phải lớn hơn 0.");
        }

        String sql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?,
                last_updated = CURRENT_TIMESTAMP
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
              AND quantity >= ?
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, quantity);
            pst.setString(2, productId.trim());
            pst.setString(3, storeId.trim());
            pst.setInt(4, quantity);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException(
                        "Sản phẩm " + productId + " tại chi nhánh " + storeId
                        + " không đủ tồn kho hoặc không tồn tại."
                );
            }

            return rowsAffected;
        }
    }

    public int addStock(Connection con, String productId, String storeId, int quantity, String unit) throws SQLException {
        if (productId == null || productId.trim().isEmpty()) {
            throw new SQLException("Mã sản phẩm không hợp lệ.");
        }

        if (storeId == null || storeId.trim().isEmpty()) {
            throw new SQLException("Thiếu store_id khi cộng tồn kho.");
        }

        if (quantity <= 0) {
            throw new SQLException("Số lượng nhập kho phải lớn hơn 0.");
        }

        String sqlUpdate = """
            UPDATE INVENTORY
            SET quantity = NVL(quantity, 0) + ?,
                unit = ?,
                last_updated = CURRENT_TIMESTAMP,
                is_deleted = 0
            WHERE product_id = ?
              AND store_id = ?
        """;

        try (PreparedStatement pst = con.prepareStatement(sqlUpdate)) {
            pst.setInt(1, quantity);
            pst.setString(2, unit == null || unit.trim().isEmpty() ? "Cái" : unit.trim());
            pst.setString(3, productId.trim());
            pst.setString(4, storeId.trim());

            int rows = pst.executeUpdate();

            if (rows > 0) {
                return rows;
            }
        }

        String sqlInsert = """
            INSERT INTO INVENTORY (
                product_id,
                store_id,
                quantity,
                unit,
                last_updated,
                is_deleted
            )
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 0)
        """;

        try (PreparedStatement pst = con.prepareStatement(sqlInsert)) {
            pst.setString(1, productId.trim());
            pst.setString(2, storeId.trim());
            pst.setInt(3, quantity);
            pst.setString(4, unit == null || unit.trim().isEmpty() ? "Cái" : unit.trim());

            return pst.executeUpdate();
        }
    }

    private Inventory mapInventory(ResultSet rs) throws SQLException {
        Inventory inv = new Inventory();

        inv.setProductId(rs.getString("product_id"));
        inv.setStoreId(rs.getString("store_id"));
        inv.setQuantity(rs.getInt("quantity"));
        inv.setUnit(rs.getString("unit"));

        try {
            inv.setLastUpdated(rs.getDate("last_updated"));
        } catch (Exception ignored) {
        }

        inv.setIsDeleted(rs.getInt("is_deleted"));

        return inv;
    }
}