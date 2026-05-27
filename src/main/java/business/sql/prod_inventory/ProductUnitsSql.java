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

        /*
         * Nhập kho thủ công đang đi qua ProductsSql.addStockWithConn(..., unit, ...).
         * Một số sản phẩm cũ có PRODUCT_UNITS là "Túi", "Thùng"... nhưng INVENTORY/unit
         * hoặc Product fallback vẫn đang là "Cái". Khi bấm Nhập kho tay, hệ thống không nên
         * chặn chỉ vì unit fallback "Cái" chưa nằm trong PRODUCT_UNITS.
         *
         * Với các đơn vị chung/chưa map rõ như Cái/Đơn vị, coi là đơn vị gốc tạm thời = 1.
         * Các đơn vị nghiệp vụ thật như Thùng/Lốc/Hộp vẫn phải được cấu hình đúng tỷ lệ.
         */
        if (isGenericBaseUnitFallback(unitId) || isGenericBaseUnitFallback(resolvedUnitId)) {
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

    private boolean isGenericBaseUnitFallback(String unitIdOrName) {
        if (unitIdOrName == null || unitIdOrName.trim().isEmpty()) {
            return true;
        }

        String normalized = java.text.Normalizer
                .normalize(unitIdOrName.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase();

        return normalized.equals("CAI")
                || normalized.equals("U_CAI")
                || normalized.equals("DON_VI")
                || normalized.equals("U_DON_VI")
                || normalized.equals("UNIT")
                || normalized.equals("U_UNIT");
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

        if (productId == null || productId.trim().isEmpty()) {
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
            if (id != null && !id.trim().isEmpty() && !ids.contains(id.trim())) {
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
            ps.setString(1, productId);
            ps.setString(2, unitId);
            ps.setBigDecimal(3, conversionRateToBase == null ? BigDecimal.ONE : conversionRateToBase);
            ps.setBigDecimal(4, sellingPrice);
            ps.setInt(5, isBaseUnit ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void upsertProductUnitWithConn(Connection con, String productId, String unitId,
            BigDecimal conversionRateToBase, boolean isBaseUnit) throws SQLException {
        upsertProductUnitWithConn(con, productId, unitId, conversionRateToBase, null, isBaseUnit);
    }

    public void setBaseUnitWithConn(Connection con, String productId, String unitId) throws SQLException {
        String resetSql = """
            UPDATE PRODUCT_UNITS
            SET is_base_unit = 0
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

    ... (truncated)