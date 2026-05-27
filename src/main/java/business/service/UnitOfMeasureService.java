package business.service;

import business.sql.prod_inventory.ProductUnitsSql;
import business.sql.prod_inventory.UnitsSql;
import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class UnitOfMeasureService {

    public boolean configureProductUnit(String productId, String unitName,
            BigDecimal conversionRateToBase, boolean isBaseUnit) {

        return configureProductUnit(productId, unitName, conversionRateToBase, null, isBaseUnit);
    }

    public boolean configureProductUnit(String productId, String unitName,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) {

        if (isBlank(productId)
                || isBlank(unitName)
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
                String unitId = UnitsSql.getInstance().ensureUnitWithConn(con, unitName);

                ProductUnitsSql.getInstance().upsertProductUnitWithConn(
                        con,
                        productId,
                        unitId,
                        conversionRateToBase,
                        sellingPrice,
                        isBaseUnit
                );

                if (isBaseUnit) {
                    ProductUnitsSql.getInstance().setBaseUnitWithConn(con, productId, unitId);
                }

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                System.err.println("Loi UnitOfMeasureService.configureProductUnit: " + e.getMessage());
                e.printStackTrace();
                return false;

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Loi ket noi UnitOfMeasureService: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProductUnit(String productId, String oldUnitId, String newUnitName,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice, boolean isBaseUnit) {

        if (isBlank(productId)
                || isBlank(oldUnitId)
                || isBlank(newUnitName)
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
                String newUnitId = UnitsSql.getInstance().ensureUnitWithConn(con, newUnitName);

                boolean ok = ProductUnitsSql.getInstance().updateProductUnitWithConnStyle(
                        productId,
                        oldUnitId,
                        newUnitId,
                        conversionRateToBase,
                        sellingPrice,
                        isBaseUnit
                );

                if (!ok) {
                    con.rollback();
                    return false;
                }

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                System.err.println("Loi UnitOfMeasureService.updateProductUnit: " + e.getMessage());
                e.printStackTrace();
                return false;

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Loi ket noi UnitOfMeasureService.updateProductUnit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteProductUnit(String productId, String unitId) {
        if (isBlank(productId) || isBlank(unitId)) {
            return false;
        }

        return ProductUnitsSql.getInstance().softDeleteProductUnit(productId, unitId);
    }

    public int convertToBaseQuantity(String productId, String unitId, int quantity) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return ProductUnitsSql.getInstance()
                    .convertToBaseQuantityWithConn(con, productId, unitId, quantity);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
