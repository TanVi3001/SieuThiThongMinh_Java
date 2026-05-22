package business.service;

import business.sql.prod_inventory.ProductsSql;
import common.db.DatabaseConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public class ProductImportService {

    private static final String DEFAULT_SUPPLIER_ID = "SUP_01";
    private static final String DEFAULT_UNIT_ID = "UN_01";
    private static final String DEFAULT_UNIT_NAME = "Cái";

    public static class ImportResult {

        public String receiptId;
        public int totalRows;
        public int successRows;
        public int skippedRows;
        public BigDecimal totalBeforeTax = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal totalAfterTax = BigDecimal.ZERO;
    }

    /**
     * Giữ lại hàm cũ để các màn hình đang gọi importProductCSV(...) không bị
     * lỗi. Nhưng bên trong sẽ chạy flow mới: import CSV + tạo phiếu nhập.
     */
    public void importProductCSV(String filePath, Consumer<Integer> progressCallback) {
        importProductCSVWithReceipt(filePath, progressCallback);
    }

    /**
     * Flow: 1. Lấy store_id từ session hiện tại. 2. Tạo PURCHASE_RECEIPTS theo
     * store_id đó. 3. Đọc CSV. 4. Tạo/cập nhật PRODUCTS master. 5. Cộng
     * INVENTORY theo product_id + store_id. 6. Active STORE_PRODUCTS theo
     * product_id + store_id. 7. Ghi PURCHASE_RECEIPT_DETAILS. 8. Ghi
     * INVENTORY_TRANSACTIONS.
     */
    public ImportResult importProductCSVWithReceipt(String filePath, Consumer<Integer> progressCallback) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("Không tìm thấy file CSV: " + file.getAbsolutePath());
        }

        String currentStoreId = getCurrentStoreIdOrThrow();

        ImportResult result = new ImportResult();
        result.receiptId = "PNCSV" + System.currentTimeMillis();

        String sqlInsertReceipt = """
            INSERT INTO PURCHASE_RECEIPTS (
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
            VALUES (?, ?, ?, ?, ?, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
        """;

        String sqlUpdateReceiptTotal = """
            UPDATE PURCHASE_RECEIPTS
            SET total_before_tax = ?,
                total_tax = ?,
                total_after_tax = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE receipt_id = ?
        """;

        String sqlCheckProduct = """
            SELECT product_id
            FROM PRODUCTS
            WHERE LOWER(TRIM(product_name)) = LOWER(TRIM(?))
              AND NVL(is_deleted, 0) = 0
            FETCH FIRST 1 ROWS ONLY
        """;

        String sqlInsertProduct = """
            INSERT INTO PRODUCTS (
                product_id,
                product_name,
                base_price,
                category_id,
                supplier_id,
                base_unit_id,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, 0)
        """;

        String sqlUpdateProduct = """
            UPDATE PRODUCTS
            SET base_price = ?,
                category_id = ?,
                supplier_id = ?
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlCheckInventory = """
            SELECT quantity
            FROM INVENTORY
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlUpdateInventory = """
            UPDATE INVENTORY
            SET quantity = NVL(quantity, 0) + ?,
                unit = ?,
                last_updated = SYSDATE,
                is_deleted = 0
            WHERE product_id = ?
              AND store_id = ?
        """;

        String sqlInsertInventory = """
            INSERT INTO INVENTORY (
                product_id,
                store_id,
                quantity,
                unit,
                last_updated,
                is_deleted
            )
            VALUES (?, ?, ?, ?, SYSDATE, 0)
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

        String sqlInsertDetail = """
            INSERT INTO PURCHASE_RECEIPT_DETAILS (
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
            VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        String sqlInsertTransaction = """
            INSERT INTO INVENTORY_TRANSACTIONS (
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
            VALUES (
                ?, ?, ?, 'INBOUND',
                ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        try (
                Connection conn = DatabaseConnection.getConnection(); BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        )) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement psInsertReceipt = conn.prepareStatement(sqlInsertReceipt); PreparedStatement psUpdateReceiptTotal = conn.prepareStatement(sqlUpdateReceiptTotal); PreparedStatement psCheckProduct = conn.prepareStatement(sqlCheckProduct); PreparedStatement psInsertProduct = conn.prepareStatement(sqlInsertProduct); PreparedStatement psUpdateProduct = conn.prepareStatement(sqlUpdateProduct); PreparedStatement psCheckInventory = conn.prepareStatement(sqlCheckInventory); PreparedStatement psUpdateInventory = conn.prepareStatement(sqlUpdateInventory); PreparedStatement psInsertInventory = conn.prepareStatement(sqlInsertInventory); PreparedStatement psUpsertStoreProduct = conn.prepareStatement(sqlUpsertStoreProduct); PreparedStatement psInsertDetail = conn.prepareStatement(sqlInsertDetail); PreparedStatement psInsertTransaction = conn.prepareStatement(sqlInsertTransaction)) {
                psInsertReceipt.setString(1, result.receiptId);
                psInsertReceipt.setString(2, DEFAULT_SUPPLIER_ID);
                psInsertReceipt.setString(3, currentStoreId);
                psInsertReceipt.setString(4, getCurrentAccountId());
                psInsertReceipt.setString(5, "Phiếu nhập tự động từ CSV: " + file.getName());
                psInsertReceipt.executeUpdate();

                String line;
                boolean isFirstLine = true;
                int rowIndex = 0;
                int totalLines = countDataLines(file);

                while ((line = br.readLine()) != null) {
                    if (isFirstLine) {
                        if (line.startsWith("\uFEFF")) {
                            line = line.substring(1);
                        }

                        isFirstLine = false;

                        if (looksLikeHeader(line)) {
                            continue;
                        }
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    result.totalRows++;
                    rowIndex++;

                    try {
                        CsvProductRow row = parseCsvRow(line);

                        if (!isValidBasicRow(row)) {
                            result.skippedRows++;
                            System.err.println("Bỏ qua dòng CSV không hợp lệ: " + line);
                            continue;
                        }

                        if (row.importPriceBeforeVat == null) {
                            row.importPriceBeforeVat = row.salePrice
                                    .multiply(new BigDecimal("0.70"))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }

                        if (row.vatRate == null) {
                            row.vatRate = InventoryPricePolicyService.resolveVatRateByCategory(row.categoryId);
                        }

                        try {
                            InventoryPricePolicyService.validateImportPriceLessThanSalePrice(
                                    row.importPriceBeforeVat,
                                    row.vatRate,
                                    row.salePrice
                            );
                        } catch (IllegalArgumentException invalidPrice) {
                            result.skippedRows++;
                            System.err.println("Bỏ qua dòng CSV vì sai logic giá: " + row.productName);
                            System.err.println(invalidPrice.getMessage());
                            continue;
                        }

                        String productId = findOrCreateProduct(
                                row,
                                psCheckProduct,
                                psInsertProduct,
                                psUpdateProduct
                        );

                        if (productId == null || productId.isBlank()) {
                            result.skippedRows++;
                            System.err.println("Không lấy được product_id cho dòng: " + line);
                            continue;
                        }

                        increaseInventory(
                                productId,
                                row.quantity,
                                currentStoreId,
                                psCheckInventory,
                                psUpdateInventory,
                                psInsertInventory
                        );

                        upsertStoreProduct(
                                psUpsertStoreProduct,
                                currentStoreId,
                                productId,
                                row.salePrice
                        );

                        BigDecimal lineBeforeTax = InventoryPricePolicyService.calculateLineBeforeTax(
                                row.importPriceBeforeVat,
                                row.quantity
                        );

                        BigDecimal lineTax = InventoryPricePolicyService.calculateLineTax(
                                lineBeforeTax,
                                row.vatRate
                        );

                        BigDecimal lineAfterTax = InventoryPricePolicyService.calculateLineAfterTax(
                                lineBeforeTax,
                                lineTax
                        );

                        insertReceiptDetail(
                                psInsertDetail,
                                result.receiptId,
                                productId,
                                row,
                                lineBeforeTax,
                                lineTax,
                                lineAfterTax
                        );

                        insertInventoryTransaction(
                                psInsertTransaction,
                                result.receiptId,
                                productId,
                                row,
                                currentStoreId,
                                lineTax,
                                lineAfterTax,
                                file.getName()
                        );

                        result.successRows++;
                        result.totalBeforeTax = result.totalBeforeTax.add(lineBeforeTax);
                        result.totalTax = result.totalTax.add(lineTax);
                        result.totalAfterTax = result.totalAfterTax.add(lineAfterTax);

                        if (progressCallback != null && totalLines > 0) {
                            int progress = Math.min(99, rowIndex * 100 / totalLines);
                            progressCallback.accept(progress);
                        }

                    } catch (Exception rowEx) {
                        result.skippedRows++;
                        System.err.println("Lỗi xử lý dòng CSV: " + line);
                        rowEx.printStackTrace();
                    }
                }

                psUpdateReceiptTotal.setBigDecimal(1, result.totalBeforeTax);
                psUpdateReceiptTotal.setBigDecimal(2, result.totalTax);
                psUpdateReceiptTotal.setBigDecimal(3, result.totalAfterTax);
                psUpdateReceiptTotal.setString(4, result.receiptId);
                psUpdateReceiptTotal.executeUpdate();

                conn.commit();

                if (progressCallback != null) {
                    progressCallback.accept(100);
                }

                System.out.println("Import CSV hoàn tất.");
                System.out.println("Chi nhánh nhập: " + currentStoreId);
                System.out.println("Mã phiếu nhập: " + result.receiptId);
                System.out.println("Tổng dòng: " + result.totalRows);
                System.out.println("Thành công: " + result.successRows);
                System.out.println("Bỏ qua: " + result.skippedRows);

                return result;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi import CSV có phiếu nhập: " + e.getMessage(), e);
        }
    }

    private boolean isValidBasicRow(CsvProductRow row) {
        if (row == null) {
            return false;
        }

        if (row.productName == null || row.productName.isBlank()) {
            return false;
        }

        if (row.salePrice == null || row.salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return row.quantity > 0;
    }

    private String findOrCreateProduct(
            CsvProductRow row,
            PreparedStatement psCheckProduct,
            PreparedStatement psInsertProduct,
            PreparedStatement psUpdateProduct
    ) throws SQLException {
        String productId = null;

        psCheckProduct.setString(1, row.productName);

        try (ResultSet rs = psCheckProduct.executeQuery()) {
            if (rs.next()) {
                productId = rs.getString("product_id");
            }
        }

        if (productId != null && !productId.isBlank()) {
            psUpdateProduct.setBigDecimal(1, row.salePrice);
            psUpdateProduct.setString(2, row.categoryId);
            psUpdateProduct.setString(3, row.supplierId);
            psUpdateProduct.setString(4, productId);
            psUpdateProduct.executeUpdate();

            return productId;
        }

        productId = ProductsSql.getInstance().generateNextProductId();

        psInsertProduct.setString(1, productId);
        psInsertProduct.setString(2, row.productName);
        psInsertProduct.setBigDecimal(3, row.salePrice);
        psInsertProduct.setString(4, row.categoryId);
        psInsertProduct.setString(5, row.supplierId);
        psInsertProduct.setString(6, DEFAULT_UNIT_ID);
        psInsertProduct.executeUpdate();

        return productId;
    }

    private void increaseInventory(
            String productId,
            int quantity,
            String storeId,
            PreparedStatement psCheckInventory,
            PreparedStatement psUpdateInventory,
            PreparedStatement psInsertInventory
    ) throws SQLException {
        boolean exists;

        psCheckInventory.setString(1, productId);
        psCheckInventory.setString(2, storeId);

        try (ResultSet rs = psCheckInventory.executeQuery()) {
            exists = rs.next();
        }

        if (exists) {
            psUpdateInventory.setInt(1, quantity);
            psUpdateInventory.setString(2, DEFAULT_UNIT_NAME);
            psUpdateInventory.setString(3, productId);
            psUpdateInventory.setString(4, storeId);
            psUpdateInventory.executeUpdate();
        } else {
            psInsertInventory.setString(1, productId);
            psInsertInventory.setString(2, storeId);
            psInsertInventory.setInt(3, quantity);
            psInsertInventory.setString(4, DEFAULT_UNIT_NAME);
            psInsertInventory.executeUpdate();
        }
    }

    private void upsertStoreProduct(
            PreparedStatement ps,
            String storeId,
            String productId,
            BigDecimal sellingPrice
    ) throws SQLException {
        ps.setString(1, storeId);
        ps.setString(2, productId);
        ps.setBigDecimal(3, sellingPrice);
        ps.executeUpdate();
    }

    private void insertReceiptDetail(
            PreparedStatement ps,
            String receiptId,
            String productId,
            CsvProductRow row,
            BigDecimal lineBeforeTax,
            BigDecimal lineTax,
            BigDecimal lineAfterTax
    ) throws SQLException {
        ps.setString(1, "PNDCSV" + System.nanoTime());
        ps.setString(2, receiptId);
        ps.setString(3, productId);
        ps.setInt(4, row.quantity);
        ps.setString(5, DEFAULT_UNIT_NAME);
        ps.setBigDecimal(6, row.importPriceBeforeVat);
        ps.setBigDecimal(7, row.salePrice);
        ps.setBigDecimal(8, row.vatRate);
        ps.setBigDecimal(9, lineBeforeTax);
        ps.setBigDecimal(10, lineTax);
        ps.setBigDecimal(11, lineAfterTax);
        ps.executeUpdate();
    }

    private void insertInventoryTransaction(
            PreparedStatement ps,
            String receiptId,
            String productId,
            CsvProductRow row,
            String storeId,
            BigDecimal lineTax,
            BigDecimal lineAfterTax,
            String fileName
    ) throws SQLException {
        ps.setString(1, "IVTCSV" + System.nanoTime());
        ps.setString(2, receiptId);
        ps.setString(3, productId);
        ps.setInt(4, row.quantity);
        ps.setString(5, DEFAULT_UNIT_NAME);
        ps.setString(6, storeId);
        ps.setBigDecimal(7, row.importPriceBeforeVat);
        ps.setBigDecimal(8, row.salePrice);
        ps.setBigDecimal(9, row.vatRate);
        ps.setBigDecimal(10, lineTax);
        ps.setBigDecimal(11, lineAfterTax);
        ps.setString(12, "Nhập CSV từ file: " + fileName);
        ps.setString(13, getCurrentAccountId());
        ps.executeUpdate();
    }

    private CsvProductRow parseCsvRow(String line) {
        String[] data = line.split(",", -1);

        if (data.length < 4) {
            throw new IllegalArgumentException(
                    "CSV phải có ít nhất 4 cột: product_name,sale_price,quantity,category_id"
            );
        }

        CsvProductRow row = new CsvProductRow();

        String productName = clean(data[0]);
        String salePriceRaw = clean(data[1]);
        String quantityRaw = clean(data[2]);
        String categoryId = clean(data[3]);

        row.productName = productName;
        row.salePrice = parseMoney(salePriceRaw);
        row.quantity = Integer.parseInt(quantityRaw);
        row.categoryId = categoryId.isEmpty() ? "CAT001" : categoryId;

        if (data.length >= 5) {
            String importPriceRaw = clean(data[4]);
            if (!importPriceRaw.isEmpty()) {
                row.importPriceBeforeVat = parseMoney(importPriceRaw);
            }
        }

        if (data.length >= 6) {
            String supplierId = clean(data[5]);
            row.supplierId = supplierId.isEmpty() ? DEFAULT_SUPPLIER_ID : supplierId;
        } else {
            row.supplierId = DEFAULT_SUPPLIER_ID;
        }

        if (data.length >= 7) {
            String vatRaw = clean(data[6]).replace("%", "");
            if (!vatRaw.isEmpty()) {
                row.vatRate = parseMoney(vatRaw);
            }
        }

        return row;
    }

    private BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }

        String normalized = raw
                .trim()
                .replace("\"", "")
                .replace(",", "")
                .replace("VNĐ", "")
                .replace("VND", "")
                .trim();

        return new BigDecimal(normalized);
    }

    private boolean looksLikeHeader(String line) {
        String s = line == null ? "" : line.toLowerCase();

        return s.contains("product")
                || s.contains("tên")
                || s.contains("ten")
                || s.contains("gia")
                || s.contains("price");
    }

    private int countDataLines(File file) {
        int count = 0;
        boolean first = true;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;

                    if (looksLikeHeader(line)) {
                        continue;
                    }
                }

                if (!line.trim().isEmpty()) {
                    count++;
                }
            }
        } catch (Exception ignored) {
        }

        return Math.max(count, 1);
    }

    private String getCurrentStoreIdOrThrow() {
        String storeId = null;

        try {
            storeId = SessionManager.getCurrentStoreId();
        } catch (Exception ignored) {
        }

        if (storeId == null || storeId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập lại bằng tài khoản đã được phân chi nhánh."
            );
        }

        return storeId.trim();
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

    private String clean(String s) {
        return s == null ? "" : s.trim().replace("\"", "");
    }

    private static class CsvProductRow {

        String productName;
        BigDecimal salePrice;
        int quantity;
        String categoryId;
        BigDecimal importPriceBeforeVat;
        String supplierId = DEFAULT_SUPPLIER_ID;
        BigDecimal vatRate;
    }
}
