package business.sql.prod_inventory;

import business.service.SessionManager;
import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.product.Product;

public class InventoryTransactionSql {

    private static InventoryTransactionSql instance;

    private InventoryTransactionSql() {
    }

    public static InventoryTransactionSql getInstance() {
        if (instance == null) {
            instance = new InventoryTransactionSql();
        }
        return instance;
    }

    public static class InventoryTransactionDTO {

        public String transactionId;
        public String receiptId;
        public String productId;
        public String productName;
        public String transactionType;
        public int quantity;
        public String unit;
        public String storeId;
        public BigDecimal unitImportPrice;
        public BigDecimal salePrice;
        public BigDecimal vatRate;
        public BigDecimal vatAmount;
        public BigDecimal totalAmount;
        public String note;
        public String createdBy;
        public Timestamp createdAt;
    }

    public static class PurchaseReceiptDTO {

        public String receiptId;
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
        public String note;
        public Timestamp createdAt;
    }

    public static class PurchaseReceiptLineDTO {

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
    }

    public String createPurchaseReceiptAndIncreaseStock(
            String productId,
            int quantity,
            BigDecimal unitImportPrice,
            BigDecimal vatRate,
            String supplierId,
            String note
    ) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        }

        if (unitImportPrice == null || unitImportPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá nhập chưa VAT phải lớn hơn 0.");
        }

        String storeId = requireCurrentStoreId();

        Product product = ProductsSql.getInstance().findById(productId);

        if (product == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId);
        }

        BigDecimal salePrice = product.getBasePrice();

        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sản phẩm chưa có giá bán hợp lệ.");
        }

        if (vatRate == null) {
            vatRate = BigDecimal.ZERO;
        }

        unitImportPrice = unitImportPrice.setScale(2, RoundingMode.HALF_UP);
        vatRate = vatRate.setScale(2, RoundingMode.HALF_UP);

        BigDecimal unitImportAfterVat = calculateImportPriceAfterVat(unitImportPrice, vatRate);

        if (unitImportAfterVat.compareTo(salePrice) >= 0) {
            throw new IllegalArgumentException(
                    "Giá nhập sau VAT phải nhỏ hơn giá bán.\n\n"
                    + "Giá nhập chưa VAT: " + money(unitImportPrice) + " VNĐ\n"
                    + "VAT: " + vatRate.stripTrailingZeros().toPlainString() + "%\n"
                    + "Giá nhập sau VAT: " + money(unitImportAfterVat) + " VNĐ\n"
                    + "Giá bán hiện tại: " + money(salePrice) + " VNĐ"
            );
        }

        BigDecimal qtyBD = BigDecimal.valueOf(quantity);

        BigDecimal beforeTax = unitImportPrice
                .multiply(qtyBD)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = beforeTax
                .multiply(vatRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal afterTax = beforeTax
                .add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        String receiptId = "PN" + System.currentTimeMillis();
        String detailId = "PND" + System.nanoTime();
        String transactionId = "IVT" + System.nanoTime();

        String unit = product.getUnit() == null || product.getUnit().trim().isEmpty()
                ? "Cái"
                : product.getUnit().trim();

        String cleanSupplierId = emptyToDefault(supplierId, "SUP_01");
        String createdBy = getCurrentAccountId();
        String cleanNote = note == null ? "" : note.trim();

        String sqlReceipt = """
            INSERT INTO PURCHASE_RECEIPTS
            (
                receipt_id,
                supplier_id,
                store_id,
                created_by,
                note,
                total_before_tax,
                total_tax,
                total_after_tax,
                created_at,
                updated_at,
                is_deleted
            )
            VALUES
            (
                ?, ?, ?, ?, ?,
                ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        String sqlDetail = """
            INSERT INTO PURCHASE_RECEIPT_DETAILS
            (
                receipt_detail_id,
                receipt_id,
                product_id,
                quantity,
                unit,
                unit_import_price,
                sale_price,
                vat_rate,
                line_before_tax,
                line_tax,
                line_after_tax,
                created_at,
                updated_at,
                is_deleted
            )
            VALUES
            (
                ?, ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        String sqlTransaction = """
            INSERT INTO INVENTORY_TRANSACTIONS
            (
                transaction_id,
                receipt_id,
                product_id,
                transaction_type,
                quantity,
                unit,
                store_id,
                unit_import_price,
                sale_price,
                vat_rate,
                vat_amount,
                total_amount,
                note,
                created_by,
                created_at,
                updated_at,
                is_deleted
            )
            VALUES
            (
                ?, ?, ?, 'INBOUND',
                ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        String sqlUpdateProductSupplier = """
            UPDATE PRODUCTS
            SET supplier_id = ?
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlUpsertStoreProduct = """
            MERGE INTO STORE_PRODUCTS sp
            USING (
                SELECT ? AS store_id,
                       ? AS product_id,
                       ? AS selling_price
                FROM dual
            ) src
            ON (
                sp.store_id = src.store_id
                AND sp.product_id = src.product_id
            )
            WHEN MATCHED THEN
                UPDATE SET
                    sp.selling_price = src.selling_price,
                    sp.is_active = 1,
                    sp.is_deleted = 0,
                    sp.updated_at = CURRENT_TIMESTAMP
            WHEN NOT MATCHED THEN
                INSERT (
                    store_id,
                    product_id,
                    selling_price,
                    is_active,
                    min_stock,
                    is_deleted,
                    created_at,
                    updated_at
                )
                VALUES (
                    src.store_id,
                    src.product_id,
                    src.selling_price,
                    1,
                    0,
                    0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (
                    PreparedStatement psReceipt = con.prepareStatement(sqlReceipt); PreparedStatement psDetail = con.prepareStatement(sqlDetail); PreparedStatement psTransaction = con.prepareStatement(sqlTransaction); PreparedStatement psUpdateProductSupplier = con.prepareStatement(sqlUpdateProductSupplier); PreparedStatement psUpsertStoreProduct = con.prepareStatement(sqlUpsertStoreProduct)) {
                psReceipt.setString(1, receiptId);
                psReceipt.setString(2, cleanSupplierId);
                psReceipt.setString(3, storeId);
                psReceipt.setString(4, createdBy);
                psReceipt.setString(5, cleanNote);
                psReceipt.setBigDecimal(6, beforeTax);
                psReceipt.setBigDecimal(7, taxAmount);
                psReceipt.setBigDecimal(8, afterTax);
                psReceipt.executeUpdate();

                psDetail.setString(1, detailId);
                psDetail.setString(2, receiptId);
                psDetail.setString(3, productId);
                psDetail.setInt(4, quantity);
                psDetail.setString(5, unit);
                psDetail.setBigDecimal(6, unitImportPrice);
                psDetail.setBigDecimal(7, salePrice);
                psDetail.setBigDecimal(8, vatRate);
                psDetail.setBigDecimal(9, beforeTax);
                psDetail.setBigDecimal(10, taxAmount);
                psDetail.setBigDecimal(11, afterTax);
                psDetail.executeUpdate();

                ProductsSql.getInstance().addStockWithConn(con, productId, quantity, unit, storeId);

                psUpsertStoreProduct.setString(1, storeId);
                psUpsertStoreProduct.setString(2, productId);
                psUpsertStoreProduct.setBigDecimal(3, salePrice);
                psUpsertStoreProduct.executeUpdate();

                psTransaction.setString(1, transactionId);
                psTransaction.setString(2, receiptId);
                psTransaction.setString(3, productId);
                psTransaction.setInt(4, quantity);
                psTransaction.setString(5, unit);
                psTransaction.setString(6, storeId);
                psTransaction.setBigDecimal(7, unitImportPrice);
                psTransaction.setBigDecimal(8, salePrice);
                psTransaction.setBigDecimal(9, vatRate);
                psTransaction.setBigDecimal(10, taxAmount);
                psTransaction.setBigDecimal(11, afterTax);
                psTransaction.setString(12, cleanNote);
                psTransaction.setString(13, createdBy);
                psTransaction.executeUpdate();

                psUpdateProductSupplier.setString(1, cleanSupplierId);
                psUpdateProductSupplier.setString(2, productId);
                psUpdateProductSupplier.executeUpdate();

                con.commit();
                return receiptId;

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo phiếu nhập: " + e.getMessage(), e);
        }
    }

    public boolean createOutboundTransaction(String productId, int quantity, String note) {
        if (productId == null || productId.trim().isEmpty() || quantity <= 0) {
            return false;
        }

        String storeId = requireCurrentStoreId();

        Product product = ProductsSql.getInstance().findByIdInStore(productId.trim(), storeId);

        if (product == null) {
            return false;
        }

        String transactionId = "IVT" + System.nanoTime();

        String unit = product.getUnit() == null || product.getUnit().trim().isEmpty()
                ? "Cái"
                : product.getUnit().trim();

        BigDecimal salePrice = product.getBasePrice() == null
                ? BigDecimal.ZERO
                : product.getBasePrice();

        BigDecimal totalAmount = salePrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        String sql = """
            INSERT INTO INVENTORY_TRANSACTIONS
            (
                transaction_id,
                product_id,
                transaction_type,
                quantity,
                unit,
                store_id,
                sale_price,
                total_amount,
                note,
                created_by,
                created_at,
                updated_at,
                is_deleted
            )
            VALUES
            (
                ?, ?, 'OUTBOUND',
                ?, ?, ?,
                ?, ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ProductsSql.getInstance().subtractStockWithConn(con, productId.trim(), quantity, unit, storeId);

                ps.setString(1, transactionId);
                ps.setString(2, productId.trim());
                ps.setInt(3, quantity);
                ps.setString(4, unit);
                ps.setString(5, storeId);
                ps.setBigDecimal(6, salePrice);
                ps.setBigDecimal(7, totalAmount);
                ps.setString(8, note == null ? "" : note.trim());
                ps.setString(9, getCurrentAccountId());
                ps.executeUpdate();

                con.commit();
                return true;

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<InventoryTransactionDTO> getRecentTransactions(int limit) {
        List<InventoryTransactionDTO> list = new ArrayList<>();

        if (limit <= 0) {
            limit = 50;
        }

        String storeId = currentStoreIdOrNull();
        boolean scoped = !SessionManager.isAdmin()
                && storeId != null
                && !storeId.isBlank();

        String sql = """
            SELECT *
            FROM (
                SELECT t.transaction_id,
                       t.receipt_id,
                       t.product_id,
                       p.product_name,
                       t.transaction_type,
                       t.quantity,
                       t.unit,
                       t.store_id,
                       t.unit_import_price,
                       t.sale_price,
                       t.vat_rate,
                       t.vat_amount,
                       t.total_amount,
                       t.note,
                       t.created_by,
                       t.created_at
                FROM INVENTORY_TRANSACTIONS t
                LEFT JOIN PRODUCTS p
                    ON p.product_id = t.product_id
                WHERE NVL(t.is_deleted, 0) = 0
        """ + (scoped ? " AND t.store_id = ? " : "") + """
                ORDER BY t.created_at DESC
            )
            WHERE ROWNUM <= ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;

            if (scoped) {
                ps.setString(idx++, storeId);
            }

            ps.setInt(idx, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryTransactionDTO dto = new InventoryTransactionDTO();

                    dto.transactionId = rs.getString("transaction_id");
                    dto.receiptId = rs.getString("receipt_id");
                    dto.productId = rs.getString("product_id");
                    dto.productName = rs.getString("product_name");
                    dto.transactionType = rs.getString("transaction_type");
                    dto.quantity = rs.getInt("quantity");
                    dto.unit = rs.getString("unit");
                    dto.storeId = rs.getString("store_id");
                    dto.unitImportPrice = rs.getBigDecimal("unit_import_price");
                    dto.salePrice = rs.getBigDecimal("sale_price");
                    dto.vatRate = rs.getBigDecimal("vat_rate");
                    dto.vatAmount = rs.getBigDecimal("vat_amount");
                    dto.totalAmount = rs.getBigDecimal("total_amount");
                    dto.note = rs.getString("note");
                    dto.createdBy = rs.getString("created_by");
                    dto.createdAt = rs.getTimestamp("created_at");

                    list.add(dto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public PurchaseReceiptDTO getReceiptDetail(String receiptId) {
        if (receiptId == null || receiptId.trim().isEmpty()) {
            return null;
        }

        String storeId = currentStoreIdOrNull();
        boolean scoped = !SessionManager.isAdmin()
                && storeId != null
                && !storeId.isBlank();

        String sql = """
            SELECT r.receipt_id,
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
                   r.note,
                   r.created_at
            FROM PURCHASE_RECEIPTS r
            JOIN PURCHASE_RECEIPT_DETAILS d
                ON d.receipt_id = r.receipt_id
            LEFT JOIN PRODUCTS p
                ON p.product_id = d.product_id
            WHERE r.receipt_id = ?
              AND NVL(r.is_deleted, 0) = 0
              AND NVL(d.is_deleted, 0) = 0
        """ + (scoped ? " AND r.store_id = ? " : "") + """
            FETCH FIRST 1 ROWS ONLY
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, receiptId.trim());

            if (scoped) {
                ps.setString(idx++, storeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PurchaseReceiptDTO dto = new PurchaseReceiptDTO();

                    dto.receiptId = rs.getString("receipt_id");
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
                    dto.note = rs.getString("note");
                    dto.createdAt = rs.getTimestamp("created_at");

                    return dto;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<PurchaseReceiptLineDTO> getReceiptLines(String receiptId) {
        List<PurchaseReceiptLineDTO> list = new ArrayList<>();

        if (receiptId == null || receiptId.trim().isEmpty()) {
            return list;
        }

        String storeId = currentStoreIdOrNull();
        boolean scoped = !SessionManager.isAdmin()
                && storeId != null
                && !storeId.isBlank();

        String sql = """
            SELECT d.product_id,
                   p.product_name,
                   d.quantity,
                   d.unit,
                   d.unit_import_price,
                   d.sale_price,
                   d.vat_rate,
                   d.line_before_tax,
                   d.line_tax,
                   d.line_after_tax
            FROM PURCHASE_RECEIPT_DETAILS d
            JOIN PURCHASE_RECEIPTS r
                ON r.receipt_id = d.receipt_id
            LEFT JOIN PRODUCTS p
                ON p.product_id = d.product_id
            WHERE d.receipt_id = ?
              AND NVL(d.is_deleted, 0) = 0
              AND NVL(r.is_deleted, 0) = 0
        """ + (scoped ? " AND r.store_id = ? " : "") + """
            ORDER BY d.created_at ASC
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, receiptId.trim());

            if (scoped) {
                ps.setString(idx++, storeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseReceiptLineDTO dto = new PurchaseReceiptLineDTO();

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

                    list.add(dto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private BigDecimal calculateImportPriceAfterVat(BigDecimal importPriceBeforeVat, BigDecimal vatRate) {
        if (importPriceBeforeVat == null) {
            importPriceBeforeVat = BigDecimal.ZERO;
        }

        if (vatRate == null) {
            vatRate = BigDecimal.ZERO;
        }

        return importPriceBeforeVat
                .multiply(
                        BigDecimal.ONE.add(
                                vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        )
                )
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String requireCurrentStoreId() {
        String storeId = currentStoreIdOrNull();

        if (storeId == null || storeId.isBlank()) {
            throw new IllegalStateException(
                    "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập lại bằng tài khoản đã được phân chi nhánh."
            );
        }

        return storeId;
    }

    private String currentStoreIdOrNull() {
        try {
            String storeId = SessionManager.getCurrentStoreId();
            return storeId == null || storeId.trim().isEmpty() ? null : storeId.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getCurrentAccountId() {
        try {
            if (SessionManager.getCurrentUser() != null) {
                return SessionManager.getCurrentUser().getAccountId();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }
}
