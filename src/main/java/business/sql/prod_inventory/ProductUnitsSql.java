package business.sql.prod_inventory;

import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.product.ProductUnit;

public class ProductUnitsSql {

    private static ProductUnitsSql instance;

    private ProductUnitsSql() {
    }

    public static ProductUnitsSql getInstance() {
        if (instance == null) {
            instance = new ProductUnitsSql();
        }
        return instance;
    }

    public void ensureBaseUnitWithConn(Connection con, String productId, String unitName) throws SQLException {
        if (productId == null || productId.isBlank()) {
            return;
        }

        try {
            String unitId = UnitsSql.getInstance().ensureUnitWithConn(con, unitName);

            upsertProductUnitWithConn(
                    con,
                    productId,
                    unitId,
                    BigDecimal.ONE,
                    null,
                    true
            );

            setBaseUnitWithConn(con, productId, unitId);

        } catch (SQLException e) {
            if (isMissingUomSchema(e)) {
                System.err.println("Bo qua tao don vi tinh mac dinh vi DB chua co UoM schema: " + e.getMessage());
                return;
            }
            throw e;
        }
    }

    public int convertToBaseQuantityWithConn(Connection con, String productId, String unitId, int quantity)
            throws SQLException {

        if (quantity < 0) {
            throw new SQLException("So luong khong duoc am.");
        }

        if (unitId == null || unitId.isBlank()) {
            return quantity;
        }

        try {
            BigDecimal rate = findRateToBaseWithConn(con, productId, unitId);
            BigDecimal baseQuantity = BigDecimal.valueOf(quantity).multiply(rate);
            return baseQuantity.setScale(0, RoundingMode.CEILING).intValueExact();

        } catch (SQLException e) {
            if (isMissingUomSchema(e)) {
                System.err.println("Bo qua quy doi don vi vi DB chua co UoM schema: " + e.getMessage());
                return quantity;
            }
            throw e;
        }
    }

    public BigDecimal findRateToBaseWithConn(Connection con, String productId, String unitId) throws SQLException {
        String resolvedUnitId = resolveUnitIdWithConn(con, unitId);

        String sql = """
            SELECT conversion_rate_to_base
            FROM PRODUCT_UNITS
            WHERE product_id = ?
              AND unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.setString(2, resolvedUnitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("conversion_rate_to_base");

                    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new SQLException("Ty le quy doi khong hop le cho san pham " + productId);
                    }

                    return rate;
                }
            }
        }

        BigDecimal rateByName = findRateToBaseByUnitNameWithConn(con, productId, unitId);
        if (rateByName != null) {
            return rateByName;
        }

        BigDecimal rateByGeneratedId = findRateToBaseByGeneratedUnitIdWithConn(con, productId, unitId);
        if (rateByGeneratedId != null) {
            return rateByGeneratedId;
        }

        if (!hasConfiguredUnitsWithConn(con, productId) || isProductBaseUnitWithConn(con, productId, unitId)) {
            return BigDecimal.ONE;
        }

        throw new SQLException("Chua cau hinh don vi " + unitId + " cho san pham " + productId);
    }

    public String resolveUnitIdWithConn(Connection con, String unitIdOrName) throws SQLException {
        if (unitIdOrName == null || unitIdOrName.isBlank()) {
            return null;
        }

        String exactSql = """
            SELECT unit_id
            FROM UNITS
            WHERE unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(exactSql)) {
            pst.setString(1, unitIdOrName);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit_id");
                }
            }
        }

        String byNameSql = """
            SELECT unit_id
            FROM UNITS
            WHERE LOWER(unit_name) = LOWER(?)
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(byNameSql)) {
            pst.setString(1, unitIdOrName);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit_id");
                }
            }
        }

        String generatedUnitId = generateUnitId(unitIdOrName);

        try (PreparedStatement pst = con.prepareStatement(exactSql)) {
            pst.setString(1, generatedUnitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit_id");
                }
            }
        }

        return unitIdOrName;
    }

    private BigDecimal findRateToBaseByUnitNameWithConn(Connection con, String productId, String unitName)
            throws SQLException {

        String sql = """
            SELECT pu.conversion_rate_to_base
            FROM PRODUCT_UNITS pu
            JOIN UNITS u ON pu.unit_id = u.unit_id
            WHERE pu.product_id = ?
              AND LOWER(u.unit_name) = LOWER(?)
              AND NVL(pu.is_deleted, 0) = 0
              AND NVL(u.is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.setString(2, unitName);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("conversion_rate_to_base");

                    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new SQLException("Ty le quy doi khong hop le cho san pham " + productId);
                    }

                    return rate;
                }
            }
        }

        return null;
    }

    private BigDecimal findRateToBaseByGeneratedUnitIdWithConn(Connection con, String productId, String unitName)
            throws SQLException {

        String generatedUnitId = generateUnitId(unitName);

        String sql = """
            SELECT conversion_rate_to_base
            FROM PRODUCT_UNITS
            WHERE product_id = ?
              AND unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.setString(2, generatedUnitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("conversion_rate_to_base");

                    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new SQLException("Ty le quy doi khong hop le cho san pham " + productId);
                    }

                    return rate;
                }
            }
        }

        return null;
    }

    private boolean hasConfiguredUnitsWithConn(Connection con, String productId) throws SQLException {
        String sql = """
            SELECT 1
            FROM PRODUCT_UNITS
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isProductBaseUnitWithConn(Connection con, String productId, String unitIdOrName)
            throws SQLException {

        String sql = """
            SELECT 1
            FROM PRODUCTS p
            LEFT JOIN UNITS u ON p.base_unit_id = u.unit_id
            WHERE p.product_id = ?
              AND (
                    p.base_unit_id = ?
                 OR p.base_unit_id = ?
                 OR LOWER(u.unit_name) = LOWER(?)
              )
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.setString(2, unitIdOrName);
            pst.setString(3, generateUnitId(unitIdOrName));
            pst.setString(4, unitIdOrName);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String generateUnitId(String unitName) {
        String normalized = java.text.Normalizer
                .normalize(unitName == null ? "" : unitName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase();

        if (normalized.isBlank()) {
            normalized = "UNIT";
        }

        if (normalized.length() > 30) {
            normalized = normalized.substring(0, 30);
        }

        return "U_" + normalized;
    }

    public List<ProductUnit> selectByProductId(String productId) {
        List<ProductUnit> units = new ArrayList<>();

        String sql = """
            SELECT
                pu.product_id,
                pu.unit_id,
                u.unit_name,
                pu.conversion_rate_to_base,
                NVL(pu.selling_price, p.base_price) AS selling_price,
                pu.is_base_unit,
                pu.is_deleted
            FROM PRODUCT_UNITS pu
            JOIN PRODUCTS p
                ON p.product_id = pu.product_id
            JOIN UNITS u
                ON u.unit_id = pu.unit_id
            WHERE pu.product_id = ?
              AND NVL(pu.is_deleted, 0) = 0
              AND NVL(u.is_deleted, 0) = 0
            ORDER BY pu.is_base_unit DESC, pu.unit_id
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    units.add(new ProductUnit(
                            rs.getString("product_id"),
                            rs.getString("unit_id"),
                            rs.getString("unit_name"),
                            rs.getBigDecimal("conversion_rate_to_base"),
                            rs.getBigDecimal("selling_price"),
                            rs.getInt("is_base_unit"),
                            rs.getInt("is_deleted")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Loi ProductUnitsSql.selectByProductId: " + e.getMessage());
            e.printStackTrace();
        }

        return units;
    }

    public void upsertProductUnitWithConn(Connection con, String productId, String unitId,
            BigDecimal conversionRateToBase, boolean isBaseUnit) throws SQLException {

        upsertProductUnitWithConn(con, productId, unitId, conversionRateToBase, null, isBaseUnit);
    }

    public void upsertProductUnitWithConn(Connection con, String productId, String unitId,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) throws SQLException {

        if (isBaseUnit) {
            clearBaseUnitFlagsWithConn(con, productId);
        }

        String sql = """
            MERGE INTO PRODUCT_UNITS pu
            USING (
                SELECT
                    ? AS product_id,
                    ? AS unit_id,
                    ? AS conversion_rate_to_base,
                    ? AS selling_price,
                    ? AS is_base_unit
                FROM dual
            ) src
            ON (
                pu.product_id = src.product_id
                AND pu.unit_id = src.unit_id
            )
            WHEN MATCHED THEN
                UPDATE SET
                    pu.conversion_rate_to_base = src.conversion_rate_to_base,
                    pu.selling_price = NVL(src.selling_price, pu.selling_price),
                    pu.is_base_unit = src.is_base_unit,
                    pu.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (
                    product_id,
                    unit_id,
                    conversion_rate_to_base,
                    selling_price,
                    is_base_unit,
                    is_deleted
                )
                VALUES (
                    src.product_id,
                    src.unit_id,
                    src.conversion_rate_to_base,
                    src.selling_price,
                    src.is_base_unit,
                    0
                )
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            BigDecimal rate = conversionRateToBase == null ? BigDecimal.ONE : conversionRateToBase;
            int base = isBaseUnit ? 1 : 0;

            pst.setString(1, productId);
            pst.setString(2, unitId);
            pst.setBigDecimal(3, rate);
            pst.setBigDecimal(4, sellingPrice);
            pst.setInt(5, base);

            pst.executeUpdate();
        }
    }

    private void clearBaseUnitFlagsWithConn(Connection con, String productId) throws SQLException {
        String sql = """
            UPDATE PRODUCT_UNITS
            SET is_base_unit = 0
            WHERE product_id = ?
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, productId);
            pst.executeUpdate();
        }
    }

    public void setBaseUnitWithConn(Connection con, String productId, String unitId) throws SQLException {
        String sql = """
            UPDATE PRODUCTS
            SET base_unit_id = ?
            WHERE product_id = ?
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, unitId);
            pst.setString(2, productId);
            pst.executeUpdate();
        }
    }

    private boolean isMissingUomSchema(SQLException e) {
        return e.getErrorCode() == 942 || e.getErrorCode() == 904;
    }

    public boolean updateProductUnitWithConnStyle(String productId, String oldUnitId,
            String newUnitId, BigDecimal conversionRateToBase,
            BigDecimal sellingPrice, boolean isBaseUnit) {

        if (productId == null || productId.trim().isEmpty()
                || oldUnitId == null || oldUnitId.trim().isEmpty()
                || newUnitId == null || newUnitId.trim().isEmpty()
                || conversionRateToBase == null
                || conversionRateToBase.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                if (isBaseUnit) {
                    clearBaseUnitFlagsWithConn(con, productId);
                }

                if (oldUnitId.equals(newUnitId)) {
                    String updateSql = """
                    UPDATE PRODUCT_UNITS
                    SET conversion_rate_to_base = ?,
                        selling_price = ?,
                        is_base_unit = ?,
                        is_deleted = 0
                    WHERE product_id = ?
                      AND unit_id = ?
                """;

                    try (PreparedStatement pst = con.prepareStatement(updateSql)) {
                        pst.setBigDecimal(1, conversionRateToBase);
                        pst.setBigDecimal(2, sellingPrice);
                        pst.setInt(3, isBaseUnit ? 1 : 0);
                        pst.setString(4, productId);
                        pst.setString(5, oldUnitId);
                        pst.executeUpdate();
                    }

                } else {
                    upsertProductUnitWithConn(
                            con,
                            productId,
                            newUnitId,
                            conversionRateToBase,
                            sellingPrice,
                            isBaseUnit
                    );

                    String deleteOldSql = """
                    UPDATE PRODUCT_UNITS
                    SET is_deleted = 1,
                        is_base_unit = 0
                    WHERE product_id = ?
                      AND unit_id = ?
                """;

                    try (PreparedStatement pst = con.prepareStatement(deleteOldSql)) {
                        pst.setString(1, productId);
                        pst.setString(2, oldUnitId);
                        pst.executeUpdate();
                    }
                }

                if (isBaseUnit) {
                    setBaseUnitWithConn(con, productId, newUnitId);
                }

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                System.err.println("Loi ProductUnitsSql.updateProductUnitWithConnStyle: " + e.getMessage());
                e.printStackTrace();
                return false;

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Loi ket noi ProductUnitsSql.updateProductUnitWithConnStyle: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean softDeleteProductUnit(String productId, String unitId) {
        if (productId == null || productId.trim().isEmpty()
                || unitId == null || unitId.trim().isEmpty()) {
            return false;
        }

        String countSql = """
        SELECT COUNT(*)
        FROM PRODUCT_UNITS
        WHERE product_id = ?
          AND NVL(is_deleted, 0) = 0
    """;

        String checkBaseSql = """
        SELECT NVL(is_base_unit, 0) AS is_base_unit
        FROM PRODUCT_UNITS
        WHERE product_id = ?
          AND unit_id = ?
          AND NVL(is_deleted, 0) = 0
    """;

        String deleteSql = """
        UPDATE PRODUCT_UNITS
        SET is_deleted = 1,
            is_base_unit = 0
        WHERE product_id = ?
          AND unit_id = ?
    """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int activeCount = 0;

                try (PreparedStatement pst = con.prepareStatement(countSql)) {
                    pst.setString(1, productId);

                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            activeCount = rs.getInt(1);
                        }
                    }
                }

                if (activeCount <= 1) {
                    throw new SQLException("Không thể xóa đơn vị cuối cùng của sản phẩm.");
                }

                int isBaseUnit = 0;

                try (PreparedStatement pst = con.prepareStatement(checkBaseSql)) {
                    pst.setString(1, productId);
                    pst.setString(2, unitId);

                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            isBaseUnit = rs.getInt("is_base_unit");
                        } else {
                            throw new SQLException("Không tìm thấy đơn vị cần xóa.");
                        }
                    }
                }

                if (isBaseUnit == 1) {
                    throw new SQLException("Không thể xóa đơn vị gốc. Hãy chọn đơn vị khác làm gốc trước.");
                }

                try (PreparedStatement pst = con.prepareStatement(deleteSql)) {
                    pst.setString(1, productId);
                    pst.setString(2, unitId);
                    int affected = pst.executeUpdate();

                    if (affected <= 0) {
                        throw new SQLException("Không xóa được đơn vị.");
                    }
                }

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                System.err.println("Loi ProductUnitsSql.softDeleteProductUnit: " + e.getMessage());
                e.printStackTrace();
                return false;

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Loi ket noi ProductUnitsSql.softDeleteProductUnit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
