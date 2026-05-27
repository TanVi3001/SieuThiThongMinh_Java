package business.sql.prod_inventory;

import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        if (isBlank(productId)) {
            return;
        }

        try {
            String cleanUnitName = isBlank(unitName) ? "Cái" : unitName.trim();
            String unitId = UnitsSql.getInstance().ensureUnitWithConn(con, cleanUnitName);

            upsertProductUnitWithConn(
                    con,
                    productId.trim(),
                    unitId,
                    BigDecimal.ONE,
                    null,
                    true
            );

            setBaseUnitWithConn(con, productId.trim(), unitId);

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

        if (isBlank(unitId)) {
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
        if (isBlank(productId)) {
            return BigDecimal.ONE;
        }

        if (isBlank(unitId)) {
            return BigDecimal.ONE;
        }

        String cleanProductId = productId.trim();
        String cleanUnitId = unitId.trim();
        String resolvedUnitId = resolveUnitIdWithConn(con, cleanUnitId);

        String sql = """
            SELECT conversion_rate_to_base
            FROM PRODUCT_UNITS
            WHERE product_id = ?
              AND unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, cleanProductId);
            pst.setString(2, resolvedUnitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("conversion_rate_to_base");
                    validateRate(rate, cleanProductId);
                    return rate;
                }
            }
        }

        BigDecimal rateByName = findRateToBaseByUnitNameWithConn(con, cleanProductId, cleanUnitId);
        if (rateByName != null) {
            return rateByName;
        }

        BigDecimal rateByGeneratedId = findRateToBaseByGeneratedUnitIdWithConn(con, cleanProductId, cleanUnitId);
        if (rateByGeneratedId != null) {
            return rateByGeneratedId;
        }

        if (!hasConfiguredUnitsWithConn(con, cleanProductId)
                || isProductBaseUnitWithConn(con, cleanProductId, cleanUnitId)
                || isProductBaseUnitWithConn(con, cleanProductId, resolvedUnitId)) {
            return BigDecimal.ONE;
        }

        // Khi nhập kho tay, INVENTORY/unit cũ có thể là Cái trong khi PRODUCT_UNITS đã cấu hình Túi/Thùng.
        // Cái/Đơn vị/Unit là fallback chung, cho qua tỷ lệ 1 để không chặn nhập kho thủ công.
        if (isGenericBaseUnitFallback(cleanUnitId) || isGenericBaseUnitFallback(resolvedUnitId)) {
            return BigDecimal.ONE;
        }

        throw new SQLException("Chua cau hinh don vi " + cleanUnitId + " cho san pham " + cleanProductId);
    }

    public String resolveUnitIdWithConn(Connection con, String unitIdOrName) throws SQLException {
        if (isBlank(unitIdOrName)) {
            return null;
        }

        String clean = unitIdOrName.trim();

        String exactSql = """
            SELECT unit_id
            FROM UNITS
            WHERE unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement pst = con.prepareStatement(exactSql)) {
            pst.setString(1, clean);

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
            pst.setString(1, clean);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit_id");
                }
            }
        }

        String generatedUnitId = generateUnitId(clean);

        try (PreparedStatement pst = con.prepareStatement(exactSql)) {
            pst.setString(1, generatedUnitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit_id");
                }
            }
        }

        return clean;
    }

    private BigDecimal findRateToBaseByUnitNameWithConn(Connection con, String productId, String unitName)
            throws SQLException {

        if (isBlank(unitName)) {
            return null;
        }

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
            pst.setString(2, unitName.trim());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("conversion_rate_to_base");
                    validateRate(rate, productId);
                    return rate;
                }
            }
        }

        return null;
    }

    private BigDecimal findRateToBaseByGeneratedUnitIdWithConn(Connection con, String productId, String unitName)
            throws SQLException {

        if (isBlank(unitName)) {
            return null;
        }

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
                    validateRate(rate, productId);
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

        if (isBlank(unitIdOrName)) {
            return false;
        }

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
            pst.setString(2, unitIdOrName.trim());
            pst.setString(3, generateUnitId(unitIdOrName));
            pst.setString(4, unitIdOrName.trim());

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isGenericBaseUnitFallback(String unitIdOrName) {
        if (isBlank(unitIdOrName)) {
            return true;
        }

        String normalized = normalizeToken(unitIdOrName);

        return normalized.equals("CAI")
                || normalized.equals("U_CAI")
                || normalized.equals("DON_VI")
                || normalized.equals("U_DON_VI")
                || normalized.equals("UNIT")
                || normalized.equals("U_UNIT");
    }

    private String generateUnitId(String unitName) {
        String normalized = normalizeToken(unitName);

        if (normalized.isBlank()) {
            normalized = "UNIT";
        }

        if (normalized.length() > 28) {
            normalized = normalized.substring(0, 28);
        }

        return "U_" + normalized;
    }

    public List<ProductUnit> selectByProductId(String productId) {
        List<ProductUnit> units = new ArrayList<>();

        if (isBlank(productId)) {
            return units;
        }

        Map<String, List<ProductUnit>> grouped = selectByProductIds(java.util.List.of(productId.trim()));
        List<ProductUnit> found = grouped.get(productId.trim());

        if (found != null) {
            units.addAll(found);
        }

        return units;
    }

    public Map<String, List<ProductUnit>> selectByProductIds(Collection<String> productIds) {
        Map<String, List<ProductUnit>> groupedUnits = new LinkedHashMap<>();

        if (productIds == null || productIds.isEmpty()) {
            return groupedUnits;
        }

        List<String> ids = new ArrayList<>();
        for (String id : productIds) {
            if (!isBlank(id) && !ids.contains(id.trim())) {
                ids.add(id.trim());
                groupedUnits.put(id.trim(), new ArrayList<>());
            }
        }

        if (ids.isEmpty()) {
            return groupedUnits;
        }

        String placeholders = ids.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));

        String sql = """
            SELECT pu.product_id,
                   pu.unit_id,
                   u.unit_name,
                   pu.conversion_rate_to_base,
                   pu.selling_price,
                   pu.is_base_unit,
                   pu.is_deleted
            FROM PRODUCT_UNITS pu
            JOIN UNITS u
              ON u.unit_id = pu.unit_id
            WHERE pu.product_id IN (%s)
              AND NVL(pu.is_deleted, 0) = 0
              AND NVL(u.is_deleted, 0) = 0
            ORDER BY pu.product_id, pu.is_base_unit DESC, pu.unit_id
        """.formatted(placeholders);

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setString(i + 1, ids.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductUnit unit = new ProductUnit();
                    unit.setProductId(rs.getString("product_id"));
                    unit.setUnitId(rs.getString("unit_id"));
                    unit.setUnitName(rs.getString("unit_name"));
                    unit.setConversionRateToBase(rs.getBigDecimal("conversion_rate_to_base"));
                    unit.setSellingPrice(rs.getBigDecimal("selling_price"));
                    unit.setBaseUnit(rs.getInt("is_base_unit") == 1);
                    unit.setDeleted(rs.getInt("is_deleted") == 1);

                    List<ProductUnit> list = groupedUnits.get(unit.getProductId());
                    if (list != null) {
                        list.add(unit);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return groupedUnits;
    }

    public void upsertProductUnitWithConn(Connection con, String productId, String unitId,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) throws SQLException {

        if (isBlank(productId) || isBlank(unitId)) {
            throw new SQLException("Thieu ma san pham hoac ma don vi.");
        }

        String sql = """
            MERGE INTO PRODUCT_UNITS pu
            USING (
                SELECT ? AS product_id,
                       ? AS unit_id,
                       ? AS conversion_rate_to_base,
                       ? AS selling_price,
                       ? AS is_base_unit
                FROM dual
            ) src
            ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
            WHEN MATCHED THEN
                UPDATE SET
                    pu.conversion_rate_to_base = src.conversion_rate_to_base,
                    pu.selling_price = src.selling_price,
                    pu.is_base_unit = src.is_base_unit,
                    pu.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (product_id, unit_id, conversion_rate_to_base, selling_price, is_base_unit, is_deleted)
                VALUES (src.product_id, src.unit_id, src.conversion_rate_to_base, src.selling_price, src.is_base_unit, 0)
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());
            ps.setString(2, unitId.trim());
            ps.setBigDecimal(3, conversionRateToBase == null ? BigDecimal.ONE : conversionRateToBase);
            ps.setBigDecimal(4, sellingPrice);
            ps.setInt(5, isBaseUnit ? 1 : 0);
            ps.executeUpdate();
        }

        if (isBaseUnit) {
            setBaseUnitWithConn(con, productId.trim(), unitId.trim());
        }
    }

    public void upsertProductUnitWithConn(Connection con, String productId, String unitId,
            BigDecimal conversionRateToBase, boolean isBaseUnit) throws SQLException {
        upsertProductUnitWithConn(con, productId, unitId, conversionRateToBase, null, isBaseUnit);
    }

    public void setBaseUnitWithConn(Connection con, String productId, String unitId) throws SQLException {
        if (isBlank(productId) || isBlank(unitId)) {
            return;
        }

        String resetSql = """
            UPDATE PRODUCT_UNITS
            SET is_base_unit = 0
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String setSql = """
            UPDATE PRODUCT_UNITS
            SET is_base_unit = 1,
                conversion_rate_to_base = 1
            WHERE product_id = ?
              AND unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String updateProductBaseSql = """
            UPDATE PRODUCTS
            SET base_unit_id = ?
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement psReset = con.prepareStatement(resetSql);
             PreparedStatement psSet = con.prepareStatement(setSql);
             PreparedStatement psProduct = con.prepareStatement(updateProductBaseSql)) {

            psReset.setString(1, productId.trim());
            psReset.executeUpdate();

            psSet.setString(1, productId.trim());
            psSet.setString(2, unitId.trim());
            psSet.executeUpdate();

            psProduct.setString(1, unitId.trim());
            psProduct.setString(2, productId.trim());
            psProduct.executeUpdate();
        }
    }

    public boolean updateProductUnitWithConnStyle(Connection con, String productId, String oldUnitId, String newUnitId,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) throws SQLException {

        if (con == null || isBlank(productId) || isBlank(oldUnitId) || isBlank(newUnitId)
                || conversionRateToBase == null || conversionRateToBase.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        String cleanProductId = productId.trim();
        String cleanOldUnitId = resolveUnitIdWithConn(con, oldUnitId.trim());
        String cleanNewUnitId = resolveUnitIdWithConn(con, newUnitId.trim());

        if (cleanOldUnitId.equalsIgnoreCase(cleanNewUnitId)) {
            upsertProductUnitWithConn(con, cleanProductId, cleanNewUnitId, conversionRateToBase, sellingPrice, isBaseUnit);
            return true;
        }

        softDeleteProductUnitWithConn(con, cleanProductId, cleanOldUnitId);
        upsertProductUnitWithConn(con, cleanProductId, cleanNewUnitId, conversionRateToBase, sellingPrice, isBaseUnit);

        if (isBaseUnit) {
            setBaseUnitWithConn(con, cleanProductId, cleanNewUnitId);
        }

        return true;
    }

    // Giữ lại overload cũ để các màn hình đang gọi không bị lỗi compile.
    public boolean updateProductUnitWithConnStyle(String productId, String oldUnitId, String newUnitId,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) {

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean ok = updateProductUnitWithConnStyle(
                        con,
                        productId,
                        oldUnitId,
                        newUnitId,
                        conversionRateToBase,
                        sellingPrice,
                        isBaseUnit
                );
                con.commit();
                return ok;
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

    public boolean softDeleteProductUnit(String productId, String unitId) {
        if (isBlank(productId) || isBlank(unitId)) {
            return false;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                boolean ok = softDeleteProductUnitWithConn(con, productId.trim(), resolveUnitIdWithConn(con, unitId.trim()));
                con.commit();
                return ok;
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

    public boolean softDeleteProductUnitWithConn(Connection con, String productId, String unitId) throws SQLException {
        if (isBlank(productId) || isBlank(unitId)) {
            return false;
        }

        String resolvedUnitId = resolveUnitIdWithConn(con, unitId.trim());
        String sql = """
            UPDATE PRODUCT_UNITS
            SET is_deleted = 1,
                is_base_unit = 0
            WHERE product_id = ?
              AND unit_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productId.trim());
            ps.setString(2, resolvedUnitId);
            return ps.executeUpdate() > 0;
        }
    }

    private void validateRate(BigDecimal rate, String productId) throws SQLException {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SQLException("Ty le quy doi khong hop le cho san pham " + productId);
        }
    }

    private boolean isMissingUomSchema(SQLException e) {
        if (e == null) {
            return false;
        }

        int code = Math.abs(e.getErrorCode());
        String msg = e.getMessage() == null ? "" : e.getMessage().toUpperCase();

        return code == 942
                || code == 904
                || msg.contains("ORA-00942")
                || msg.contains("ORA-00904")
                || msg.contains("PRODUCT_UNITS")
                || msg.contains("UNITS")
                || msg.contains("BASE_UNIT_ID");
    }

    private String normalizeToken(String value) {
        return java.text.Normalizer
                .normalize(value == null ? "" : value.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("Đ", "D")
                .replaceAll("đ", "d")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
