package business.sql.prod_inventory;

import business.service.SessionManager;
import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseReceiptSql {

    private static PurchaseReceiptSql instance;

    private PurchaseReceiptSql() {
    }

    public static PurchaseReceiptSql getInstance() {
        if (instance == null) {
            instance = new PurchaseReceiptSql();
        }
        return instance;
    }

    public static class PurchaseReceiptLineDTO {

        public String receiptId;
        public String storeId;
        public String storeName;
        public String productId;
        public String productName;
        public int quantity;
        public String unit;
        public BigDecimal unitImportPrice;
        public BigDecimal salePrice;
        public BigDecimal vatRate;
        public BigDecimal beforeTax;
        public BigDecimal taxAmount;
        public BigDecimal afterTax;
        public Timestamp createdAt;
    }

    public List<PurchaseReceiptLineDTO> getReceiptLines(String receiptId) {
        return getReceiptLinesInternal(receiptId, null);
    }

    public List<PurchaseReceiptLineDTO> getReceiptLinesForCurrentScope(String receiptId) {
        String storeId = null;

        try {
            storeId = SessionManager.getScopedStoreIdOrNull();
        } catch (Exception ignored) {
        }

        if (storeId == null || storeId.trim().isEmpty()) {
            return getReceiptLinesInternal(receiptId, null);
        }

        return getReceiptLinesInternal(receiptId, storeId.trim());
    }

    public List<PurchaseReceiptLineDTO> getReceiptLinesByStore(String receiptId, String storeId) {
        if (storeId == null || storeId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return getReceiptLinesInternal(receiptId, storeId.trim());
    }

    private List<PurchaseReceiptLineDTO> getReceiptLinesInternal(String receiptId, String storeId) {
        List<PurchaseReceiptLineDTO> list = new ArrayList<>();

        if (receiptId == null || receiptId.trim().isEmpty()) {
            return list;
        }

        StringBuilder sql = new StringBuilder("""
            SELECT d.receipt_id,
                   r.store_id,
                   s.store_name,
                   d.product_id,
                   p.product_name,
                   d.quantity,
                   d.unit,
                   d.unit_import_price,
                   d.sale_price,
                   d.vat_rate,
                   d.line_before_tax,
                   d.line_tax,
                   d.line_after_tax,
                   r.created_at
            FROM PURCHASE_RECEIPT_DETAILS d
            JOIN PURCHASE_RECEIPTS r
                ON r.receipt_id = d.receipt_id
            LEFT JOIN STORES s
                ON s.store_id = r.store_id
            LEFT JOIN PRODUCTS p
                ON p.product_id = d.product_id
            WHERE d.receipt_id = ?
              AND NVL(d.is_deleted, 0) = 0
              AND NVL(r.is_deleted, 0) = 0
        """);

        if (storeId != null && !storeId.trim().isEmpty()) {
            sql.append(" AND TRIM(r.store_id) = TRIM(?) ");
        }

        sql.append(" ORDER BY d.created_at ASC ");

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setString(i++, receiptId.trim());

            if (storeId != null && !storeId.trim().isEmpty()) {
                ps.setString(i++, storeId.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseReceiptLineDTO dto = new PurchaseReceiptLineDTO();

                    dto.receiptId = rs.getString("receipt_id");
                    dto.storeId = rs.getString("store_id");
                    dto.storeName = rs.getString("store_name");
                    dto.productId = rs.getString("product_id");
                    dto.productName = rs.getString("product_name");
                    dto.quantity = rs.getInt("quantity");
                    dto.unit = rs.getString("unit");
                    dto.unitImportPrice = rs.getBigDecimal("unit_import_price");
                    dto.salePrice = rs.getBigDecimal("sale_price");
                    dto.vatRate = rs.getBigDecimal("vat_rate");
                    dto.beforeTax = rs.getBigDecimal("line_before_tax");
                    dto.taxAmount = rs.getBigDecimal("line_tax");
                    dto.afterTax = rs.getBigDecimal("line_after_tax");
                    dto.createdAt = rs.getTimestamp("created_at");

                    list.add(dto);
                }
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                return getReceiptLinesLegacy(receiptId, storeId);
            }

            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private List<PurchaseReceiptLineDTO> getReceiptLinesLegacy(String receiptId, String storeId) {
        List<PurchaseReceiptLineDTO> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT d.receipt_id,
                   CAST(NULL AS VARCHAR2(50)) AS store_id,
                   CAST(NULL AS NVARCHAR2(100)) AS store_name,
                   d.product_id,
                   p.product_name,
                   d.quantity,
                   d.unit,
                   d.unit_import_price,
                   d.sale_price,
                   d.vat_rate,
                   d.line_before_tax,
                   d.line_tax,
                   d.line_after_tax,
                   r.created_at
            FROM PURCHASE_RECEIPT_DETAILS d
            JOIN PURCHASE_RECEIPTS r
                ON r.receipt_id = d.receipt_id
            LEFT JOIN PRODUCTS p
                ON p.product_id = d.product_id
            WHERE d.receipt_id = ?
              AND NVL(d.is_deleted, 0) = 0
              AND NVL(r.is_deleted, 0) = 0
            ORDER BY d.created_at ASC
        """);

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, receiptId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseReceiptLineDTO dto = new PurchaseReceiptLineDTO();

                    dto.receiptId = rs.getString("receipt_id");
                    dto.storeId = rs.getString("store_id");
                    dto.storeName = rs.getString("store_name");
                    dto.productId = rs.getString("product_id");
                    dto.productName = rs.getString("product_name");
                    dto.quantity = rs.getInt("quantity");
                    dto.unit = rs.getString("unit");
                    dto.unitImportPrice = rs.getBigDecimal("unit_import_price");
                    dto.salePrice = rs.getBigDecimal("sale_price");
                    dto.vatRate = rs.getBigDecimal("vat_rate");
                    dto.beforeTax = rs.getBigDecimal("line_before_tax");
                    dto.taxAmount = rs.getBigDecimal("line_tax");
                    dto.afterTax = rs.getBigDecimal("line_after_tax");
                    dto.createdAt = rs.getTimestamp("created_at");

                    list.add(dto);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return list;
    }
}
