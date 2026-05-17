package business.sql.prod_inventory;

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

    public int subtractStockWithConn(Connection con, String productId, int quantity) throws SQLException {
        return subtractStockWithConn(con, productId, quantity, null);
    }

    public int subtractStockWithConn(Connection con, String productId, int quantity, String unitId) throws SQLException {
        int baseQuantity = ProductUnitsSql.getInstance()
                .convertToBaseQuantityWithConn(con, productId, unitId, quantity);
        String sql = "UPDATE INVENTORY "
                + "SET quantity = quantity - ? "
                + "WHERE product_id = ? AND quantity >= ? AND is_deleted = 0";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, baseQuantity);
            pst.setString(2, productId);
            pst.setInt(3, baseQuantity);

            int res = pst.executeUpdate();
            if (res == 0) {
                throw new SQLException("Không đủ tồn kho hoặc không tìm thấy sản phẩm: " + productId);
            }
            return res;
        }
    }

    public int addStockWithConn(Connection con, String productId, int quantity) throws SQLException {
        return addStockWithConn(con, productId, quantity, null);
    }

    public int addStockWithConn(Connection con, String productId, int quantity, String unitId) throws SQLException {
        int baseQuantity = ProductUnitsSql.getInstance()
                .convertToBaseQuantityWithConn(con, productId, unitId, quantity);
        String sql = "UPDATE INVENTORY "
                + "SET quantity = quantity + ? "
                + "WHERE product_id = ? AND is_deleted = 0";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, baseQuantity);
            pst.setString(2, productId);

            int res = pst.executeUpdate();
            if (res == 0) {
                throw new SQLException("Không tìm thấy sản phẩm để hoàn kho: " + productId);
            }
            return res;
        }
    }

    public List<Product> selectAll() {
        List<Product> list = new ArrayList<>();

        String sql = "SELECT p.product_id, p.product_name, p.base_price, "
                + "       p.category_id, p.supplier_id, CAST(NULL AS VARCHAR2(255)) AS image_path, "
                + "       i.store_id, i.quantity, i.unit "
                + "FROM PRODUCTS p "
                + "LEFT JOIN INVENTORY i ON p.product_id = i.product_id AND NVL(i.is_deleted, 0) = 0 "
                + "WHERE NVL(p.is_deleted, 0) = 0 "
                + "ORDER BY p.product_id";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getString("product_id"));
                p.setProductName(rs.getString("product_name"));
                p.setBasePrice(rs.getBigDecimal("base_price"));
                p.setCategoryId(rs.getString("category_id"));
                p.setSupplierId(rs.getString("supplier_id"));
                try { p.setImagePath(rs.getString("image_path")); } catch (Exception ignored) {}

                try {
                    p.setStoreId(rs.getString("store_id"));
                } catch (Exception ignored) {
                }
                try {
                    p.setUnit(rs.getString("unit"));
                } catch (Exception ignored) {
                }

                p.setQuantity(rs.getInt("quantity"));
                p.setIsDeleted(0);
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean insert(Product p) {
        String sqlProduct = "INSERT INTO PRODUCTS "
                + "(product_id, product_name, base_price, category_id, supplier_id, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, 0)";

        String sqlInventory = "INSERT INTO INVENTORY "
                + "(product_id, store_id, quantity, unit, last_updated, is_deleted) "
                + "VALUES (?, ?, ?, ?, SYSDATE, 0)";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInventory)) {

                if (isBlank(p.getProductId()) || isBlank(p.getProductName())
                        || p.getBasePrice() == null
                        || isBlank(p.getCategoryId())
                        || isBlank(p.getSupplierId())
                        || isBlank(safeStoreId(p))) {
                    throw new SQLException("Thiếu dữ liệu bắt buộc khi thêm sản phẩm.");
                }

                psProd.setString(1, p.getProductId());
                psProd.setString(2, p.getProductName());
                psProd.setBigDecimal(3, p.getBasePrice());
                psProd.setString(4, p.getCategoryId());
                psProd.setString(5, p.getSupplierId());
                int prodRows = psProd.executeUpdate();

                psInv.setString(1, p.getProductId());
                psInv.setString(2, safeStoreId(p));
                psInv.setInt(3, p.getQuantity());
                psInv.setString(4, safeUnit(p));
                int invRows = psInv.executeUpdate();
                ProductUnitsSql.getInstance().ensureBaseUnitWithConn(con, p.getProductId(), safeUnit(p));

                if (prodRows > 0) {
                    String newValue = joinPairs(
                            pair("product_name", p.getProductName()),
                            pair("base_price", p.getBasePrice()),
                            pair("category_id", p.getCategoryId()),
                            pair("supplier_id", p.getSupplierId()),
                            pair("store_id", safeStoreId(p)),
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
        String sqlProduct = "UPDATE PRODUCTS "
                + "SET product_name = ?, base_price = ?, category_id = ?, supplier_id = ? "
                + "WHERE product_id = ? AND is_deleted = 0";

        String sqlInventory = "UPDATE INVENTORY "
                + "SET quantity = ?, unit = ?, last_updated = SYSDATE "
                + "WHERE product_id = ? AND store_id = ? AND is_deleted = 0";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            // BƯỚC 1: Lấy giá cũ TRƯỚC KHI cập nhật để ghi Audit Log
            BigDecimal oldPrice = null;
            String sqlGetOld = "SELECT base_price FROM PRODUCTS WHERE product_id = ?";
            try (PreparedStatement psOld = con.prepareStatement(sqlGetOld)) {
                psOld.setString(1, p.getProductId());
                try (ResultSet rsOld = psOld.executeQuery()) {
                    if (rsOld.next()) {
                        oldPrice = rsOld.getBigDecimal("base_price");
                    }
                }
            }

            try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInventory)) {

                // UPDATE PRODUCTS
                psProd.setString(1, p.getProductName() != null ? p.getProductName().trim() : "");
                psProd.setBigDecimal(2, p.getBasePrice());
                psProd.setString(3, p.getCategoryId() != null ? p.getCategoryId().trim() : "");
                psProd.setString(4, p.getSupplierId() != null ? p.getSupplierId().trim() : "SUP001");
                psProd.setString(5, p.getProductId().trim());
                int prodRows = psProd.executeUpdate();

                // UPDATE INVENTORY
                psInv.setInt(1, p.getQuantity());
                psInv.setString(2, safeUnit(p));
                psInv.setString(3, p.getProductId().trim());
                psInv.setString(4, safeStoreId(p));
                int invRows = psInv.executeUpdate();

                if (invRows == 0) {
                    String insertInv = "INSERT INTO INVENTORY (product_id, store_id, quantity, unit, last_updated, is_deleted) VALUES (?, ?, ?, ?, SYSDATE, 0)";
                    try (PreparedStatement psInsInv = con.prepareStatement(insertInv)) {
                        psInsInv.setString(1, p.getProductId().trim());
                        psInsInv.setString(2, safeStoreId(p));
                        psInsInv.setInt(3, p.getQuantity());
                        psInsInv.setString(4, safeUnit(p));
                        psInsInv.executeUpdate();
                    }
                }

                // BƯỚC 2: GHI AUDIT LOG GỌN GÀNG
                if (prodRows > 0) {
                    ProductUnitsSql.getInstance().ensureBaseUnitWithConn(con, p.getProductId(), safeUnit(p));
                    try {
                        String oldValStr = "price=" + (oldPrice != null ? oldPrice.toPlainString() : "unknown");
                        String newValStr = "price=" + (p.getBasePrice() != null ? p.getBasePrice().toPlainString() : "null");

                        logAuditWithConn(con, "UPDATE_PRICE", "PRODUCT", p.getProductId(), oldValStr, newValStr, "Cập nhật giá/thông tin sản phẩm");
                    } catch (Exception auditEx) {
                        System.err.println("CẢNH BÁO: Lỗi ghi Audit Log: " + auditEx.getMessage());
                    }
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
            return false;
        }
    }

    public boolean delete(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String cleanId = productId.trim(); // Dọn dẹp khoảng trắng thừa

        // Dùng NVL để phòng hờ trường hợp is_deleted đang mang giá trị NULL trong DB
        String sqlProduct = "UPDATE PRODUCTS SET is_deleted = 1 WHERE product_id = ? AND NVL(is_deleted, 0) = 0";
        String sqlInv = "UPDATE INVENTORY SET is_deleted = 1 WHERE product_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false); // Bắt đầu Transaction

            try (PreparedStatement psProd = con.prepareStatement(sqlProduct); PreparedStatement psInv = con.prepareStatement(sqlInv)) {

                // 1. Cập nhật bảng PRODUCTS
                psProd.setString(1, cleanId);
                int prodRows = psProd.executeUpdate();

                // 2. Cập nhật bảng INVENTORY (Không tìm thấy kho cũng không sao)
                psInv.setString(1, cleanId);
                psInv.executeUpdate();

                // 3. Ghi Audit Log nếu xóa thành công
                if (prodRows > 0) {
                    logAuditWithConn(con, "DELETE_PRODUCT", "PRODUCT", cleanId, "is_deleted=0", "is_deleted=1", "Xoa mem san pham");
                } else {
                    System.err.println("CẢNH BÁO: Không tìm thấy SP [" + cleanId + "] hoặc SP đã bị xóa trước đó!");
                }

                con.commit(); // Chốt sổ thành công
                return prodRows > 0;

            } catch (Exception e) {
                con.rollback();
                System.err.println("=== LỖI LOGIC KHI XÓA SẢN PHẨM ===");
                e.printStackTrace(); // IN LỖI RA CONSOLE ĐỂ BẮT BUG
                return false;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("=== LỖI KẾT NỐI KHI XÓA SẢN PHẨM ===");
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

    public List<Product> searchByName(String name) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.product_id, p.product_name, p.base_price, "
                + "       p.category_id, p.supplier_id, "
                + "       i.store_id, i.quantity, i.unit "
                + "FROM PRODUCTS p "
                + "LEFT JOIN INVENTORY i ON p.product_id = i.product_id AND i.is_deleted = 0 "
                + "WHERE p.is_deleted = 0 AND LOWER(p.product_name) LIKE LOWER(?) "
                + "ORDER BY p.product_id";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getString("product_id"));
                    p.setProductName(rs.getString("product_name"));
                    p.setBasePrice(rs.getBigDecimal("base_price"));
                    p.setCategoryId(rs.getString("category_id"));
                    p.setSupplierId(rs.getString("supplier_id"));
                    try {
                        p.setStoreId(rs.getString("store_id"));
                    } catch (Exception ignored) {
                    }
                    try {
                        p.setUnit(rs.getString("unit"));
                    } catch (Exception ignored) {
                    }
                    p.setQuantity(rs.getInt("quantity"));
                    p.setIsDeleted(0);
                    list.add(p);
                }
            }
        } catch (SQLException e) {
        }
        return list;
    }

    public List<String> getSearchSuggestions(String keyword) {
        List<String> list = new ArrayList<>();
        String sql;
        if (keyword == null || keyword.trim().isEmpty()) {
            sql = "SELECT DISTINCT product_name FROM PRODUCTS WHERE is_deleted = 0 AND ROWNUM <= 15";
        } else {
            sql = "SELECT DISTINCT product_name FROM PRODUCTS WHERE is_deleted = 0 AND LOWER(product_name) LIKE LOWER(?) AND ROWNUM <= 15";
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
        }
        return list;
    }

    // ==========================================
    // 3 HÀM BỔ SUNG CHO GIAO DIỆN PRODUCTVIEW
    // ==========================================
    public Product findById(String productId) {
        String sql = "SELECT p.product_id, p.product_name, p.base_price, p.category_id, p.supplier_id, CAST(NULL AS VARCHAR2(255)) AS image_path, "
                + "i.store_id, i.quantity, i.unit "
                + "FROM PRODUCTS p LEFT JOIN INVENTORY i ON p.product_id = i.product_id AND i.is_deleted = 0 "
                + "WHERE p.is_deleted = 0 AND p.product_id = ? FETCH FIRST 1 ROWS ONLY";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getString("product_id"));
                    p.setProductName(rs.getString("product_name"));
                    p.setBasePrice(rs.getBigDecimal("base_price"));
                    p.setCategoryId(rs.getString("category_id"));
                    p.setSupplierId(rs.getString("supplier_id"));
                    p.setImagePath(rs.getString("image_path"));
                    try { p.setStoreId(rs.getString("store_id")); } catch (Exception ignored) {}
                    try { p.setUnit(rs.getString("unit")); } catch (Exception ignored) {}
                    p.setQuantity(rs.getInt("quantity"));
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Product findByExactName(String name) {
        String sql = "SELECT p.product_id, p.product_name, p.base_price, p.category_id, p.supplier_id, i.store_id, i.quantity, i.unit "
                + "FROM PRODUCTS p LEFT JOIN INVENTORY i ON p.product_id = i.product_id AND i.is_deleted = 0 "
                + "WHERE p.is_deleted = 0 AND LOWER(p.product_name) = LOWER(?) FETCH FIRST 1 ROWS ONLY";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getString("product_id"));
                    p.setProductName(rs.getString("product_name"));
                    p.setBasePrice(rs.getBigDecimal("base_price"));
                    p.setCategoryId(rs.getString("category_id"));
                    p.setSupplierId(rs.getString("supplier_id"));
                    try {
                        p.setStoreId(rs.getString("store_id"));
                    } catch (Exception e) {
                    }
                    p.setQuantity(rs.getInt("quantity"));
                    try {
                        p.setUnit(rs.getString("unit"));
                    } catch (Exception e) {
                    }
                    return p;
                }
            }
        } catch (SQLException e) {
        }
        return null;
    }

    public Product findByExactNameAndCategory(String name, String categoryId) {
        String sql = "SELECT p.product_id, p.product_name, p.base_price, p.category_id, p.supplier_id, i.store_id, i.quantity, i.unit "
                + "FROM PRODUCTS p LEFT JOIN INVENTORY i ON p.product_id = i.product_id AND i.is_deleted = 0 "
                + "WHERE p.is_deleted = 0 "
                + "AND LOWER(TRIM(p.product_name)) = LOWER(TRIM(?)) "
                + "AND TRIM(p.category_id) = TRIM(?) "
                + "FETCH FIRST 1 ROWS ONLY";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name != null ? name.trim() : "");
            ps.setString(2, categoryId != null ? categoryId.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getString("product_id"));
                    p.setProductName(rs.getString("product_name"));
                    p.setBasePrice(rs.getBigDecimal("base_price"));
                    p.setCategoryId(rs.getString("category_id"));
                    p.setSupplierId(rs.getString("supplier_id"));
                    try {
                        p.setStoreId(rs.getString("store_id"));
                    } catch (Exception e) {
                    }
                    p.setQuantity(rs.getInt("quantity"));
                    try {
                        p.setUnit(rs.getString("unit"));
                    } catch (Exception e) {
                    }
                    return p;
                }
            }
        } catch (SQLException e) {
        }
        return null;
    }

    public boolean addQuantity(String productId, int addedQuantity, String storeId) {
        if (storeId == null || storeId.trim().isEmpty()) {
            storeId = "ST001";
        }
        String sqlUpdate = "UPDATE INVENTORY SET quantity = quantity + ?, last_updated = SYSDATE WHERE product_id = ? AND store_id = ? AND is_deleted = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
            psUpdate.setInt(1, addedQuantity);
            psUpdate.setString(2, productId);
            psUpdate.setString(3, storeId);
            int rows = psUpdate.executeUpdate();
            if (rows == 0) {
                String sqlInsert = "INSERT INTO INVENTORY (product_id, store_id, quantity, unit, last_updated, is_deleted) VALUES (?, ?, ?, 'Cái', SYSDATE, 0)";
                try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                    psInsert.setString(1, productId);
                    psInsert.setString(2, storeId);
                    psInsert.setInt(3, addedQuantity);
                    rows = psInsert.executeUpdate();
                }
            }
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateNextProductId() {
        String sql = "SELECT MAX(product_id) FROM PRODUCTS WHERE product_id LIKE 'SP%'";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getString(1) != null) {
                int num = Integer.parseInt(rs.getString(1).substring(2));
                return String.format("SP%07d", num + 1);
            }
        } catch (Exception e) {
        }
        return "SP0000001";
    }

    // ===== helpers =====
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeStoreId(Product p) {
        try {
            String s = p.getStoreId();
            return (s == null || s.isBlank()) ? "ST001" : s.trim();
        } catch (Exception e) {
            return "ST001";
        }
    }

    private String safeUnit(Product p) {
        try {
            String u = (String) p.getUnit();
            return (u == null || u.isBlank()) ? "Cái" : u.trim();
        } catch (Exception e) {
            return "Cái";
        }
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
        log.setAccountId(business.service.SessionManager.getCurrentUser() != null ? business.service.SessionManager.getCurrentUser().getAccountId() : null);
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
}
