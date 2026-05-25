package business.sql.prod_inventory;

import business.service.RolePermissionService;
import common.db.DatabaseConnection;
import model.product.Supplier;
import business.sql.SqlInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuppliersSql implements SqlInterface<Supplier> {

    private static SuppliersSql instance;

    private SuppliersSql() {
    }

    public static SuppliersSql getInstance() {
        if (instance == null) {
            instance = new SuppliersSql();
        }
        return instance;
    }

    public static class SupplierProductStat {

        public String supplierId;
        public String supplierName;
        public int productCount;
        public int totalQuantity;
    }

    public String generateNextSupplierId() {
        String sql = """
            SELECT NVL(
                MAX(TO_NUMBER(REGEXP_SUBSTR(supplier_id, '[0-9]+$'))),
                0
            ) + 1 AS next_no
            FROM SUPPLIERS
            WHERE REGEXP_LIKE(supplier_id, '^SUP_[0-9]+$')
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int nextNo = rs.getInt("next_no");
                return String.format("SUP_%03d", nextNo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "SUP_001";
    }

    public List<SupplierProductStat> getSupplierProductStats() {
        List<SupplierProductStat> result = new ArrayList<>();

        if (!RolePermissionService.canView()) {
            return result;
        }

        String sql = """
        WITH product_supplier_link AS (
            SELECT
                p.product_id,
                p.supplier_id
            FROM PRODUCTS p
            WHERE NVL(p.is_deleted, 0) = 0
              AND p.supplier_id IS NOT NULL

            UNION

            SELECT DISTINCT
                d.product_id,
                r.supplier_id
            FROM PURCHASE_RECEIPTS r
            JOIN PURCHASE_RECEIPT_DETAILS d
                ON d.receipt_id = r.receipt_id
            WHERE NVL(r.is_deleted, 0) = 0
              AND NVL(d.is_deleted, 0) = 0
              AND r.supplier_id IS NOT NULL
        )
        SELECT
            s.supplier_id,
            s.supplier_name,
            COUNT(DISTINCT l.product_id) AS product_count,
            NVL(SUM(i.quantity), 0) AS total_quantity
        FROM SUPPLIERS s
        LEFT JOIN product_supplier_link l
            ON l.supplier_id = s.supplier_id
        LEFT JOIN INVENTORY i
            ON i.product_id = l.product_id
            AND NVL(i.is_deleted, 0) = 0
        WHERE NVL(s.is_deleted, 0) = 0
        GROUP BY s.supplier_id, s.supplier_name
        ORDER BY product_count DESC, total_quantity DESC, s.supplier_id
    """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SupplierProductStat stat = new SupplierProductStat();
                stat.supplierId = rs.getString("supplier_id");
                stat.supplierName = rs.getString("supplier_name");
                stat.productCount = rs.getInt("product_count");
                stat.totalQuantity = rs.getInt("total_quantity");
                result.add(stat);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Map<String, SupplierProductStat> getSupplierProductStatMap() {
        Map<String, SupplierProductStat> map = new HashMap<>();

        for (SupplierProductStat stat : getSupplierProductStats()) {
            map.put(stat.supplierId, stat);
        }

        return map;
    }

    @Override
    public int insert(Supplier t) {
        if (!RolePermissionService.canAdd()) {
            System.err.println("[SuppliersSql] Permission denied: add supplier");
            return 0;
        }

        if (t.getSupplierId() == null || t.getSupplierId().trim().isEmpty()) {
            t.setSupplierId(generateNextSupplierId());
        }

        String sql = """
            INSERT INTO SUPPLIERS (
                supplier_id,
                supplier_name,
                email,
                address,
                phone_number,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, 0)
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getSupplierId());
            pst.setString(2, t.getSupplierName());
            pst.setString(3, t.getEmail());
            pst.setString(4, t.getAddress());
            pst.setString(5, t.getPhoneNumber());

            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public List<Supplier> selectAll() {
        List<Supplier> result = new ArrayList<>();

        if (!RolePermissionService.canView()) {
            return result;
        }

        String sql = """
            SELECT supplier_id,
                   supplier_name,
                   email,
                   address,
                   phone_number,
                   is_deleted
            FROM SUPPLIERS
            WHERE NVL(is_deleted, 0) = 0
            ORDER BY
                CASE
                    WHEN REGEXP_LIKE(supplier_id, '^SUP_[0-9]+$')
                    THEN TO_NUMBER(REGEXP_SUBSTR(supplier_id, '[0-9]+$'))
                    ELSE 999999
                END,
                supplier_id
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                result.add(mapResultSetToSupplier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public int update(Supplier t) {
        if (!RolePermissionService.canEdit()) {
            System.err.println("[SuppliersSql] Permission denied: edit supplier");
            return 0;
        }

        String sql = """
            UPDATE SUPPLIERS
            SET supplier_name = ?,
                email = ?,
                address = ?,
                phone_number = ?,
                is_deleted = 0
            WHERE supplier_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getSupplierName());
            pst.setString(2, t.getEmail());
            pst.setString(3, t.getAddress());
            pst.setString(4, t.getPhoneNumber());
            pst.setString(5, t.getSupplierId());

            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public int delete(String id) {
        if (!RolePermissionService.canDelete()) {
            System.err.println("[SuppliersSql] Permission denied: delete supplier");
            return 0;
        }

        String sql = """
            UPDATE SUPPLIERS
            SET is_deleted = 1
            WHERE supplier_id = ?
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public Supplier selectById(String id) {
        if (!RolePermissionService.canView()) {
            return null;
        }

        String sql = """
            SELECT supplier_id,
                   supplier_name,
                   email,
                   address,
                   phone_number,
                   is_deleted
            FROM SUPPLIERS
            WHERE supplier_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSupplier(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Supplier> selectByCondition(String condition) {
        List<Supplier> result = new ArrayList<>();

        if (!RolePermissionService.canView()) {
            return result;
        }

        String sql = """
            SELECT supplier_id,
                   supplier_name,
                   email,
                   address,
                   phone_number,
                   is_deleted
            FROM SUPPLIERS
            WHERE NVL(is_deleted, 0) = 0
              AND (
                    LOWER(supplier_id) LIKE LOWER(?)
                 OR LOWER(supplier_name) LIKE LOWER(?)
                 OR LOWER(phone_number) LIKE LOWER(?)
                 OR LOWER(email) LIKE LOWER(?)
              )
            ORDER BY supplier_id
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            String pattern = "%" + condition + "%";

            pst.setString(1, pattern);
            pst.setString(2, pattern);
            pst.setString(3, pattern);
            pst.setString(4, pattern);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToSupplier(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean existsActiveSupplierName(String supplierName, String exceptSupplierId) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM SUPPLIERS
            WHERE LOWER(TRIM(supplier_name)) = LOWER(TRIM(?))
              AND NVL(is_deleted, 0) = 0
              AND (? IS NULL OR supplier_id <> ?)
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, supplierName);
            ps.setString(2, exceptSupplierId);
            ps.setString(3, exceptSupplierId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();

        supplier.setSupplierId(rs.getString("supplier_id"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setEmail(rs.getString("email"));
        supplier.setAddress(rs.getString("address"));
        supplier.setPhoneNumber(rs.getString("phone_number"));
        supplier.setIsDeleted(rs.getInt("is_deleted"));

        return supplier;
    }
}
