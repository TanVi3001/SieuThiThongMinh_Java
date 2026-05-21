package business.sql.prod_inventory;

import business.service.SessionManager;
import business.sql.rbac.AuditLogSql;
import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.product.Product;

public class ProductsSql {

    private static ProductsSql instance;

    private ProductsSql() {
    }

    public static ProductsSql getInstance() {
        if (instance == null) {
            instance = new ProductsSql();
        }
        return instance;
    }

    // =========================================================
    // STOCK OPERATIONS - ALWAYS STORE-SCOPED WHEN POSSIBLE
    // =========================================================
    public int subtractStockWithConn(Connection con, String productId, int quantity) throws SQLException {
        return subtractStockWithConn(con, productId, quantity, null, currentStoreIdOrDefault());
    }

    public int subtractStockWithConn(Connection con, String productId, int quantity, String unitId) throws SQLException {
        return subtractStockWithConn(con, productId, quantity, unitId, currentStoreIdOrDefault());
    }

    public int subtractStockWithConn(Connection con, String productId, int quantity, String unitId, String storeId) throws SQLException {
        int baseQuantity = ProductUnitsSql.getInstance()
                .convertToBaseQuantityWithConn(con, productId, unitId, quantity);

        String sql = """
            UPDATE INVENTORY
            SET quantity = quantity - ?, last_updated = SYSDATE
            WHERE product_id = ?
              AND store_id = ?
              AND quantity >= ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, baseQuantity);
            pst.setString(2, productId);
            pst.setString(3, cleanStoreId(storeId));
            pst.setInt(4, baseQuantity);

            int res = pst.executeUpdate();
            if (res == 0) {
                throw new SQLException("Không đủ tồn kho hoặc không tìm thấy sản phẩm trong chi nhánh: " + productId);
            }
            return res;
        }
    }

    public int addStockWithConn(Connection con, String productId, int quantity) throws SQLException {
        return addStockWithConn(con, productId, quantity, null, currentStoreIdOrDefault());
    }

    public int addStockWithConn(Connection con, String productId, int quantity, String unitId) throws SQLException {
        return addStockWithConn(con, productId, quantity, unitId, currentStoreIdOrDefault());
    }

    public int addStockWithConn(Connection con, String productId, int quantity, String unitId, String storeId) throws SQLException {
        int baseQuantity = ProductUnitsSql.getInstance()
                .convertToBaseQuantityWithConn(con, productId, unitId, quantity);

        String sql = """
            MERGE INTO INVENTORY i
            USING (
                SELECT ? AS product_id, ? AS store_id, ? AS quantity, ? AS unit_name FROM dual
            ) src
            ON (i.product_id = src.product_id AND i.store_id = src.store_id)
            WHEN MATCHED THEN
                UPDATE SET
                    i.quantity = NVL(i.quantity, 0) + src.quantity,
                    i.unit = src.unit_name,
                    i.last_updated = SYSDATE,
                    i.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (product_id, store_id, quantity, unit, last_updated, is_deleted)
                VALUES (src.product_id, src.store_id, src.quantity, src.unit_name, SYSDATE, 0)
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.setString(2, cleanStoreId(storeId));
            pst.setInt(3, baseQuantity);
            pst.setString(4, safeUnit(unitId));
            int res = pst.executeUpdate();
            if (res == 0) {
                throw new SQLException("Không thể cộng tồn kho cho sản phẩm: " + productId);
            }
            ensureStoreProductWithConn(con, cleanStoreId(storeId), productId, null, 1);
            return res;
        }
    }

    // =========================================================
    // SELECT
    // =========================================================
    public List<Product> selectAll() {
        String scopedStoreId = shouldScopeByCurrentStore() ? currentStoreIdOrDefault() : null;
        if (scopedStoreId != null) {
            return selectAllByStore(scopedStoreId);
        }

        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.product_id, p.product_name, p.base_price,
                   p.category_id, p.supplier_id, p.image_path,
                   i.store_id, i.quantity, i.unit, i.last_updated,
                   NVL(sp.selling_price, p.base_price) AS effective_price
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON p.product_id = i.product_id
               AND NVL(i.is_deleted, 0) = 0
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = p.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(p.is_deleted, 0) = 0
            ORDER BY p.product_id, i.store_id
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> selectAllByStore(String storeId) {
        List<Product> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT p.product_id,
               p.product_name,
               NVL(sp.selling_price, p.base_price) AS effective_price,
               p.base_price,
               p.category_id,
               p.supplier_id,
               p.image_path,
               i.store_id,
               NVL(i.quantity, 0) AS quantity,
               i.unit,
               i.last_updated
        FROM INVENTORY i
        JOIN PRODUCTS p
            ON p.product_id = i.product_id
        LEFT JOIN STORE_PRODUCTS sp
            ON sp.product_id = p.product_id
           AND sp.store_id = i.store_id
           AND NVL(sp.is_deleted, 0) = 0
        WHERE i.store_id = ?
          AND NVL(i.is_deleted, 0) = 0
          AND NVL(p.is_deleted, 0) = 0
          AND NVL(sp.is_active, 1) = 1
    """);

        if (requireImportedStockForStoreView()) {
            sql.append("""
          AND EXISTS (
                SELECT 1
                FROM PURCHASE_RECEIPTS pr
                JOIN PURCHASE_RECEIPT_DETAILS prd
                    ON pr.receipt_id = prd.receipt_id
                WHERE pr.store_id = i.store_id
                  AND prd.product_id = i.product_id
                  AND NVL(pr.is_deleted, 0) = 0
                  AND NVL(prd.is_deleted, 0) = 0
          )
        """);
        }

        sql.append(" ORDER BY p.product_id ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, cleanStoreId(storeId));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // INSERT / UPDATE / DELETE
    // =========================================================
    public boolean insert(Product p) {
        String sqlProduct = """
            INSERT INTO PRODUCTS
            (product_id, product_name, base_price, category_id, supplier_id, image_path, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, 0)
        """;

        String sqlInventory = """
            INSERT INTO INVENTORY
            (product_id, store_id, quantity, unit, last_updated, is_deleted)
            VALUES (?, ?, ?, ?, SYSDATE, 0)
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInventory)) {

                String storeId = safeStoreId(p);
                if (isBlank(p.getProductId()) || isBlank(p.getProductName())
                        || p.getBasePrice() == null
                        || isBlank(p.getCategoryId())
                        || isBlank(p.getSupplierId())
                        || isBlank(storeId)) {
                    throw new SQLException("Thiếu dữ liệu bắt buộc khi thêm sản phẩm.");
                }

                psProd.setString(1, p.getProductId());
                psProd.setString(2, p.getProductName());
                psProd.setBigDecimal(3, p.getBasePrice());
                psProd.setString(4, p.getCategoryId());
                psProd.setString(5, p.getSupplierId());
                psProd.setString(6, p.getImagePath());
                int prodRows = psProd.executeUpdate();

                psInv.setString(1, p.getProductId());
                psInv.setString(2, storeId);
                psInv.setInt(3, p.getQuantity());
                psInv.setString(4, safeUnit(p));
                int invRows = psInv.executeUpdate();

                ProductUnitsSql.getInstance().ensureBaseUnitWithConn(con, p.getProductId(), safeUnit(p));
                ensureStoreProductWithConn(con, storeId, p.getProductId(), p.getBasePrice(), 1);

                if (prodRows > 0) {
                    String newValue = joinPairs(
                            pair("product_name", p.getProductName()),
                            pair("base_price", p.getBasePrice()),
                            pair("category_id", p.getCategoryId()),
                            pair("supplier_id", p.getSupplierId()),
                            pair("store_id", storeId),
                            pair("quantity", p.getQuantity()),
                            pair("unit", safeUnit(p))
                    );
                    logAuditWithConn(con, "CREATE_PRODUCT", "PRODUCT", p.getProductId(), null, newValue, "Tao moi san pham");
                }

                con.commit();
                return prodRows > 0 && invRows > 0;
            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Product p) {
        String sqlProduct = """
            UPDATE PRODUCTS
            SET product_name = ?, base_price = ?, category_id = ?, supplier_id = ?, image_path = ?
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlInventory = """
            MERGE INTO INVENTORY i
            USING (
                SELECT ? AS product_id, ? AS store_id, ? AS quantity, ? AS unit_name FROM dual
            ) src
            ON (i.product_id = src.product_id AND i.store_id = src.store_id)
            WHEN MATCHED THEN
                UPDATE SET
                    i.quantity = src.quantity,
                    i.unit = src.unit_name,
                    i.last_updated = SYSDATE,
                    i.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (product_id, store_id, quantity, unit, last_updated, is_deleted)
                VALUES (src.product_id, src.store_id, src.quantity, src.unit_name, SYSDATE, 0)
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            BigDecimal oldPrice = null;
            try (PreparedStatement psOld = con.prepareStatement("SELECT base_price FROM PRODUCTS WHERE product_id = ?")) {
                psOld.setString(1, p.getProductId());
                try (ResultSet rsOld = psOld.executeQuery()) {
                    if (rsOld.next()) {
                        oldPrice = rsOld.getBigDecimal("base_price");
                    }
                }
            }

            try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInventory)) {

                String storeId = safeStoreId(p);

                psProd.setString(1, p.getProductName() != null ? p.getProductName().trim() : "");
                psProd.setBigDecimal(2, p.getBasePrice());
                psProd.setString(3, p.getCategoryId() != null ? p.getCategoryId().trim() : "");
                psProd.setString(4, p.getSupplierId() != null ? p.getSupplierId().trim() : "SUP001");
                psProd.setString(5, p.getImagePath());
                psProd.setString(6, p.getProductId().trim());
                int prodRows = psProd.executeUpdate();

                psInv.setString(1, p.getProductId().trim());
                psInv.setString(2, storeId);
                psInv.setInt(3, p.getQuantity());
                psInv.setString(4, safeUnit(p));
                psInv.executeUpdate();

                ensureStoreProductWithConn(con, storeId, p.getProductId(), p.getBasePrice(), 1);

                if (prodRows > 0) {
                    ProductUnitsSql.getInstance().ensureBaseUnitWithConn(con, p.getProductId(), safeUnit(p));
                    String oldValStr = "price=" + (oldPrice != null ? oldPrice.toPlainString() : "unknown");
                    String newValStr = "price=" + (p.getBasePrice() != null ? p.getBasePrice().toPlainString() : "null")
                            + ", store_id=" + storeId + ", quantity=" + p.getQuantity();
                    logAuditWithConn(con, "UPDATE_PRICE", "PRODUCT", p.getProductId(), oldValStr, newValStr, "Cập nhật giá/thông tin sản phẩm");
                }

                con.commit();
                return prodRows > 0;
            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String cleanId = productId.trim();
        String storeId = currentStoreIdOrNull();
        boolean admin = SessionManager.isAdmin();

        String sqlProduct = "UPDATE PRODUCTS SET is_deleted = 1 WHERE product_id = ? AND NVL(is_deleted, 0) = 0";
        String sqlInvAll = "UPDATE INVENTORY SET is_deleted = 1 WHERE product_id = ? AND NVL(is_deleted, 0) = 0";
        String sqlInvStore = "UPDATE INVENTORY SET is_deleted = 1 WHERE product_id = ? AND store_id = ? AND NVL(is_deleted, 0) = 0";
        String sqlStoreProductStore = "UPDATE STORE_PRODUCTS SET is_deleted = 1, is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND store_id = ? AND NVL(is_deleted, 0) = 0";
        String sqlStoreProductAll = "UPDATE STORE_PRODUCTS SET is_deleted = 1, is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int affected;
                if (admin || storeId == null || storeId.isBlank()) {
                    try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInvAll); PreparedStatement psSp = con.prepareStatement(sqlStoreProductAll)) {
                        psProd.setString(1, cleanId);
                        affected = psProd.executeUpdate();
                        psInv.setString(1, cleanId);
                        psInv.executeUpdate();
                        psSp.setString(1, cleanId);
                        psSp.executeUpdate();
                    }
                    if (affected > 0) {
                        logAuditWithConn(con, "DELETE_PRODUCT", "PRODUCT", cleanId, "is_deleted=0", "is_deleted=1", "Xoa mem san pham toan he thong");
                    }
                } else {
                    try (PreparedStatement psInv = con.prepareStatement(sqlInvStore); PreparedStatement psSp = con.prepareStatement(sqlStoreProductStore)) {
                        psInv.setString(1, cleanId);
                        psInv.setString(2, storeId);
                        affected = psInv.executeUpdate();
                        psSp.setString(1, cleanId);
                        psSp.setString(2, storeId);
                        psSp.executeUpdate();
                    }
                    if (affected > 0) {
                        logAuditWithConn(con, "DELETE_PRODUCT", "PRODUCT", cleanId, "store_id=" + storeId + ", is_deleted=0", "store_id=" + storeId + ", is_deleted=1", "Xoa mem san pham tai chi nhanh");
                    }
                }
                con.commit();
                return affected > 0;
            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUsedInOrders(String productId) {
        String sql = "SELECT COUNT(*) FROM ORDER_DETAILS WHERE product_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================================================
    // SEARCH/FIND
    // =========================================================
    public List<Product> searchByName(String name) {
        String storeId = shouldScopeByCurrentStore() ? currentStoreIdOrDefault() : null;
        if (storeId != null) {
            return searchByNameInStore(name, storeId);
        }

        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.product_id, p.product_name, p.base_price,
                   p.category_id, p.supplier_id, p.image_path,
                   i.store_id, i.quantity, i.unit, i.last_updated,
                   NVL(sp.selling_price, p.base_price) AS effective_price
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON p.product_id = i.product_id
               AND NVL(i.is_deleted, 0) = 0
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = p.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(p.is_deleted, 0) = 0
              AND LOWER(p.product_name) LIKE LOWER(?)
            ORDER BY p.product_id, i.store_id
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Product> searchByNameInStore(String name, String storeId) {
        List<Product> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT p.product_id,
               p.product_name,
               NVL(sp.selling_price, p.base_price) AS effective_price,
               p.base_price,
               p.category_id,
               p.supplier_id,
               p.image_path,
               i.store_id,
               i.quantity,
               i.unit,
               i.last_updated
        FROM INVENTORY i
        JOIN PRODUCTS p
            ON p.product_id = i.product_id
        LEFT JOIN STORE_PRODUCTS sp
            ON sp.product_id = p.product_id
           AND sp.store_id = i.store_id
           AND NVL(sp.is_deleted, 0) = 0
        WHERE i.store_id = ?
          AND NVL(i.is_deleted, 0) = 0
          AND NVL(p.is_deleted, 0) = 0
          AND NVL(sp.is_active, 1) = 1
          AND LOWER(p.product_name) LIKE LOWER(?)
    """);

        if (requireImportedStockForStoreView()) {
            sql.append("""
          AND EXISTS (
                SELECT 1
                FROM PURCHASE_RECEIPTS pr
                JOIN PURCHASE_RECEIPT_DETAILS prd
                    ON pr.receipt_id = prd.receipt_id
                WHERE pr.store_id = i.store_id
                  AND prd.product_id = i.product_id
                  AND NVL(pr.is_deleted, 0) = 0
                  AND NVL(prd.is_deleted, 0) = 0
          )
        """);
        }

        sql.append(" ORDER BY p.product_id ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, cleanStoreId(storeId));
            ps.setString(2, "%" + name + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> getSearchSuggestions(String keyword) {
        List<String> list = new ArrayList<>();
        String sql;
        if (keyword == null || keyword.trim().isEmpty()) {
            sql = "SELECT DISTINCT product_name FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0 AND ROWNUM <= 15";
        } else {
            sql = "SELECT DISTINCT product_name FROM PRODUCTS WHERE NVL(is_deleted, 0) = 0 AND LOWER(product_name) LIKE LOWER(?) AND ROWNUM <= 15";
        }

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setString(1, "%" + keyword.trim() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("product_name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Product findById(String productId) {
        String storeId = shouldScopeByCurrentStore() ? currentStoreIdOrDefault() : null;
        if (storeId != null) {
            Product scoped = findByIdInStore(productId, storeId);
            if (scoped != null) {
                return scoped;
            }
        }

        String sql = """
            SELECT p.product_id, p.product_name, p.base_price,
                   p.category_id, p.supplier_id, p.image_path,
                   i.store_id, i.quantity, i.unit, i.last_updated,
                   NVL(sp.selling_price, p.base_price) AS effective_price
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON p.product_id = i.product_id
               AND NVL(i.is_deleted, 0) = 0
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = p.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(p.is_deleted, 0) = 0
              AND p.product_id = ?
            FETCH FIRST 1 ROWS ONLY
        """;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Product findByIdInStore(String productId, String storeId) {
        StringBuilder sql = new StringBuilder("""
        SELECT p.product_id,
               p.product_name,
               NVL(sp.selling_price, p.base_price) AS effective_price,
               p.base_price,
               p.category_id,
               p.supplier_id,
               p.image_path,
               i.store_id,
               i.quantity,
               i.unit,
               i.last_updated
        FROM INVENTORY i
        JOIN PRODUCTS p
            ON p.product_id = i.product_id
        LEFT JOIN STORE_PRODUCTS sp
            ON sp.product_id = p.product_id
           AND sp.store_id = i.store_id
           AND NVL(sp.is_deleted, 0) = 0
        WHERE NVL(p.is_deleted, 0) = 0
          AND NVL(i.is_deleted, 0) = 0
          AND NVL(sp.is_active, 1) = 1
          AND p.product_id = ?
          AND i.store_id = ?
    """);

        if (requireImportedStockForStoreView()) {
            sql.append("""
          AND EXISTS (
                SELECT 1
                FROM PURCHASE_RECEIPTS pr
                JOIN PURCHASE_RECEIPT_DETAILS prd
                    ON pr.receipt_id = prd.receipt_id
                WHERE pr.store_id = i.store_id
                  AND prd.product_id = i.product_id
                  AND NVL(pr.is_deleted, 0) = 0
                  AND NVL(prd.is_deleted, 0) = 0
          )
        """);
        }

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, productId.trim());
            ps.setString(2, cleanStoreId(storeId));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Product findByExactName(String name) {
        return findByExactNameAndCategory(name, null);
    }

    public Product findByExactNameAndCategory(String name, String categoryId) {
        String storeId = currentStoreIdOrNull();
        if (storeId != null && !storeId.isBlank()) {
            Product scoped = findByExactNameAndCategory(name, categoryId, storeId);
            if (scoped != null) {
                return scoped;
            }
        }
        return findByExactNameAndCategoryGlobal(name, categoryId);
    }

    public Product findByExactNameAndCategory(String name, String categoryId, String storeId) {
        String sql = """
            SELECT p.product_id, p.product_name,
                   NVL(sp.selling_price, p.base_price) AS effective_price,
                   p.base_price,
                   p.category_id, p.supplier_id, p.image_path,
                   i.store_id, i.quantity, i.unit, i.last_updated
            FROM INVENTORY i
            JOIN PRODUCTS p
                ON p.product_id = i.product_id
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = p.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(p.is_deleted, 0) = 0
              AND NVL(i.is_deleted, 0) = 0
              AND i.store_id = ?
              AND LOWER(TRIM(p.product_name)) = LOWER(TRIM(?))
              AND (? IS NULL OR TRIM(p.category_id) = TRIM(?))
            FETCH FIRST 1 ROWS ONLY
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cleanStoreId(storeId));
            ps.setString(2, name != null ? name.trim() : "");
            ps.setString(3, categoryId == null || categoryId.isBlank() ? null : categoryId.trim());
            ps.setString(4, categoryId == null || categoryId.isBlank() ? null : categoryId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Product findByExactNameAndCategoryGlobal(String name, String categoryId) {
        String sql = """
            SELECT p.product_id, p.product_name, p.base_price,
                   p.category_id, p.supplier_id, p.image_path,
                   i.store_id, i.quantity, i.unit, i.last_updated,
                   NVL(sp.selling_price, p.base_price) AS effective_price
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON p.product_id = i.product_id
               AND NVL(i.is_deleted, 0) = 0
            LEFT JOIN STORE_PRODUCTS sp
                ON sp.product_id = p.product_id
               AND sp.store_id = i.store_id
               AND NVL(sp.is_deleted, 0) = 0
            WHERE NVL(p.is_deleted, 0) = 0
              AND LOWER(TRIM(p.product_name)) = LOWER(TRIM(?))
              AND (? IS NULL OR TRIM(p.category_id) = TRIM(?))
            FETCH FIRST 1 ROWS ONLY
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name != null ? name.trim() : "");
            ps.setString(2, categoryId == null || categoryId.isBlank() ? null : categoryId.trim());
            ps.setString(3, categoryId == null || categoryId.isBlank() ? null : categoryId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addQuantity(String productId, int addedQuantity, String storeId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int rows = addStockWithConn(con, productId, addedQuantity, null, cleanStoreId(storeId));
                con.commit();
                return rows > 0;
            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateNextProductId() {
        String sql = """
            SELECT NVL(MAX(TO_NUMBER(SUBSTR(product_id, 3))), 0) + 1 AS next_num
            FROM PRODUCTS
            WHERE REGEXP_LIKE(product_id, '^SP[0-9]+$')
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int nextNum = rs.getInt("next_num");
                return String.format("SP%07d", nextNum);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "SP0000001";
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getString("product_id"));
        p.setProductName(rs.getString("product_name"));
        try {
            p.setBasePrice(rs.getBigDecimal("effective_price"));
        } catch (Exception e) {
            p.setBasePrice(rs.getBigDecimal("base_price"));
        }
        p.setCategoryId(rs.getString("category_id"));
        p.setSupplierId(rs.getString("supplier_id"));
        try {
            p.setImagePath(rs.getString("image_path"));
        } catch (Exception ignored) {
        }
        try {
            p.setStoreId(rs.getString("store_id"));
        } catch (Exception ignored) {
        }
        try {
            p.setUnit(rs.getString("unit"));
        } catch (Exception ignored) {
        }
        try {
            p.setQuantity(rs.getInt("quantity"));
        } catch (Exception ignored) {
            p.setQuantity(0);
        }
        try {
            p.setLastUpdated(rs.getTimestamp("last_updated"));
        } catch (Exception ignored) {
        }
        p.setIsDeleted(0);
        return p;
    }

    private void ensureStoreProductWithConn(Connection con, String storeId, String productId, BigDecimal price, int isActive) throws SQLException {
        String sql = """
            MERGE INTO STORE_PRODUCTS sp
            USING (
                SELECT ? AS store_id, ? AS product_id, ? AS selling_price, ? AS is_active FROM dual
            ) src
            ON (sp.store_id = src.store_id AND sp.product_id = src.product_id)
            WHEN MATCHED THEN
                UPDATE SET
                    sp.selling_price = NVL(src.selling_price, sp.selling_price),
                    sp.is_active = src.is_active,
                    sp.is_deleted = 0,
                    sp.updated_at = CURRENT_TIMESTAMP
            WHEN NOT MATCHED THEN
                INSERT (store_id, product_id, selling_price, is_active, min_stock, is_deleted, created_at, updated_at)
                VALUES (src.store_id, src.product_id, src.selling_price, src.is_active, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cleanStoreId(storeId));
            ps.setString(2, productId);
            ps.setBigDecimal(3, price);
            ps.setInt(4, isActive);
            ps.executeUpdate();
        }
    }

    private boolean shouldScopeByCurrentStore() {
        return !SessionManager.isAdmin()
                && currentStoreIdOrNull() != null
                && !currentStoreIdOrNull().isBlank();
    }

    private String currentStoreIdOrNull() {
        try {
            return SessionManager.getCurrentStoreId();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentStoreIdOrDefault() {
        String storeId = currentStoreIdOrNull();
        return cleanStoreId(storeId);
    }

    private String cleanStoreId(String storeId) {
        if (storeId == null || storeId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập bằng tài khoản đã được phân chi nhánh."
            );
        }

        return storeId.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeStoreId(Product p) {
        try {
            String s = p.getStoreId();
            if (s != null && !s.isBlank()) {
                return s.trim();
            }
        } catch (Exception ignored) {
        }
        return currentStoreIdOrDefault();
    }

    private String safeUnit(Product p) {
        try {
            String u = p.getUnit();
            return (u == null || u.isBlank()) ? "Cái" : u.trim();
        } catch (Exception e) {
            return "Cái";
        }
    }

    private String safeUnit(String unitId) {
        return (unitId == null || unitId.isBlank()) ? "Cái" : unitId.trim();
    }

    private String pair(String col, Object val) {
        return col + "=" + (val == null ? "null" : String.valueOf(val).trim());
    }

    private String joinPairs(String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts != null) {
            for (String p : parts) {
                if (p != null && !p.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(p);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void logAuditWithConn(Connection con, String actionType, String entityType, String entityId,
            String oldValue, String newValue, String reason) throws SQLException {
        model.account.AuditLog log = new model.account.AuditLog();
        log.setAccountId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getAccountId() : null);
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setIpAddress("local");
        log.setDeviceInfo(System.getProperty("os.name") + " | Java " + System.getProperty("java.version"));
        AuditLogSql.getInstance().insertWithConn(con, log);
    }

    private boolean requireImportedStockForStoreView() {
        return !SessionManager.isAdmin();
    }
}
