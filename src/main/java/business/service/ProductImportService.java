package business.service;

import common.db.DatabaseConnection;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.function.Consumer;

public class ProductImportService {

    private static final String DEFAULT_STORE_ID = "ST01";
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

    public void importProductCSV(String filePath, Consumer<Integer> progressCallback) {
        importProductCSVWithReceipt(filePath, progressCallback);
    }

    public ImportResult importProductCSVWithReceipt(String filePath, Consumer<Integer> progressCallback) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("Không tìm thấy file CSV: " + file.getAbsolutePath());
        }

        ImportResult result = new ImportResult();
        result.receiptId = "PNCSV" + System.currentTimeMillis();

        String createdBy = getCurrentAccountId();

        String insertReceiptSql = """
            INSERT INTO PURCHASE_RECEIPTS
            (
                receipt_id,
                supplier_id,
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
                ?, ?, ?, ?,
                0, 0, 0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            )
        """;

        String updateReceiptTotalSql = """
            UPDATE PURCHASE_RECEIPTS
            SET total_before_tax = ?,
                total_tax = ?,
                total_after_tax = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE receipt_id = ?
        """;

        String checkProductSql = """
            SELECT product_id, base_price
            FROM PRODUCTS
            WHERE LOWER(TRIM(product_name)) = LOWER(TRIM(?))
              AND NVL(is_deleted, 0) = 0
            FETCH FIRST 1 ROWS ONLY
        """;

        String insertProductSql = """
            INSERT INTO PRODUCTS
            (
                product_name,
                base_price,
                category_id,
                supplier_id,
                base_unit_id,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, 0)
        """;

        String updateProductPriceSql = """
            UPDATE PRODUCTS
            SET base_price = ?,
                category_id = ?,
                supplier_id = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String checkProductAgainSql = """
            SELECT product_id
            FROM PRODUCTS
            WHERE LOWER(TRIM(product_name)) = LOWER(TRIM(?))
              AND NVL(is_deleted, 0) = 0
            FETCH FIRST 1 ROWS ONLY
        """;

        String checkInventorySql = """
            SELECT quantity
            FROM INVENTORY
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String updateInventorySql = """
            UPDATE INVENTORY
            SET quantity = quantity + ?,
                last_updated = SYSDATE
            WHERE product_id = ?
              AND store_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String insertInventorySql = """
            INSERT INTO INVENTORY
            (
                product_id,
                store_id,
                quantity,
                unit,
                last_updated,
                is_deleted
            )
            VALUES (?, ?, ?, ?, SYSDATE, 0)
        """;

        String insertDetailSql = """
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

        String insertTransactionSql = """
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

        try (
                Connection conn = DatabaseConnection.getConnection(); BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        )) {
            if (conn == null) {
                throw new SQLException("Không thể kết nối Database.");
            }

            conn.setAutoCommit(false);

            try (
                    PreparedStatement psInsertReceipt = conn.prepareStatement(insertReceiptSql); PreparedStatement psUpdateReceiptTotal = conn.prepareStatement(updateReceiptTotalSql); PreparedStatement psCheckProduct = conn.prepareStatement(checkProductSql); PreparedStatement psInsertProduct = conn.prepareStatement(insertProductSql, new String[]{"PRODUCT_ID"}); PreparedStatement psUpdateProductPrice = conn.prepareStatement(updateProductPriceSql); PreparedStatement psCheckProductAgain = conn.prepareStatement(checkProductAgainSql); PreparedStatement psCheckInventory = conn.prepareStatement(checkInventorySql); PreparedStatement psUpdateInventory = conn.prepareStatement(updateInventorySql); PreparedStatement psInsertInventory = conn.prepareStatement(insertInventorySql); PreparedStatement psInsertDetail = conn.prepareStatement(insertDetailSql); PreparedStatement psInsertTransaction = conn.prepareStatement(insertTransactionSql)) {
                psInsertReceipt.setString(1, result.receiptId);
                psInsertReceipt.setString(2, DEFAULT_SUPPLIER_ID);
                psInsertReceipt.setString(3, createdBy);
                psInsertReceipt.setString(4, "Phiếu nhập tự động từ CSV: " + file.getName());
                psInsertReceipt.executeUpdate();

                int totalLines = countDataLines(file);
                String line;
                boolean isFirstLine = true;
                int rowIndex = 0;

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

                        if (row.productName.isEmpty()) {
                            result.skippedRows++;
                            continue;
                        }

                        if (row.quantity <= 0) {
                            result.skippedRows++;
                            System.err.println("Bỏ qua dòng vì số lượng <= 0: " + line);
                            continue;
                        }

                        if (row.salePrice.compareTo(BigDecimal.ZERO) <= 0) {
                            result.skippedRows++;
                            System.err.println("Bỏ qua dòng vì giá bán không hợp lệ: " + line);
                            continue;
                        }

                        if (row.importPriceBeforeVat == null) {
                            row.importPriceBeforeVat = row.salePrice
                                    .multiply(new BigDecimal("0.70"))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }

                        if (row.vatRate == null) {
                            row.vatRate = resolveVatRateByCategory(row.categoryId);
                        }

                        BigDecimal importPriceAfterVat = row.importPriceBeforeVat
                                .multiply(BigDecimal.ONE.add(row.vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                                .setScale(2, RoundingMode.HALF_UP);

                        if (importPriceAfterVat.compareTo(row.salePrice) >= 0) {
                            result.skippedRows++;
                            System.err.println(
                                    "Bỏ qua dòng vì giá nhập sau VAT >= giá bán: "
                                    + row.productName
                                    + " | nhập sau VAT=" + importPriceAfterVat
                                    + " | giá bán=" + row.salePrice
                            );
                            continue;
                        }

                        String productId = findOrCreateProduct(
                                row,
                                psCheckProduct,
                                psInsertProduct,
                                psUpdateProductPrice,
                                psCheckProductAgain
                        );

                        if (productId == null || productId.trim().isEmpty()) {
                            result.skippedRows++;
                            System.err.println("Không lấy được product_id cho dòng: " + line);
                            continue;
                        }

                        increaseInventory(
                                productId,
                                row.quantity,
                                psCheckInventory,
                                psUpdateInventory,
                                psInsertInventory
                        );

                        BigDecimal quantityBD = BigDecimal.valueOf(row.quantity);

                        BigDecimal lineBeforeTax = row.importPriceBeforeVat
                                .multiply(quantityBD)
                                .setScale(2, RoundingMode.HALF_UP);

                        BigDecimal lineTax = lineBeforeTax
                                .multiply(row.vatRate)
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                        BigDecimal lineAfterTax = lineBeforeTax
                                .add(lineTax)
                                .setScale(2, RoundingMode.HALF_UP);

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
                                lineTax,
                                lineAfterTax,
                                createdBy,
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

                System.out.println(
                        "Import CSV hoàn tất. Phiếu: " + result.receiptId
                        + " | thành công: " + result.successRows
                        + " | bỏ qua: " + result.skippedRows
                );

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

    private String findOrCreateProduct(
            CsvProductRow row,
            PreparedStatement psCheckProduct,
            PreparedStatement psInsertProduct,
            PreparedStatement psUpdateProductPrice,
            PreparedStatement psCheckProductAgain
    ) throws SQLException {
        String productId = null;

        psCheckProduct.setString(1, row.productName);

        try (ResultSet rs = psCheckProduct.executeQuery()) {
            if (rs.next()) {
                productId = rs.getString("product_id");
            }
        }

        if (productId != null) {
            psUpdateProductPrice.setBigDecimal(1, row.salePrice);
            psUpdateProductPrice.setString(2, row.categoryId);
            psUpdateProductPrice.setString(3, row.supplierId);
            psUpdateProductPrice.setString(4, productId);
            psUpdateProductPrice.executeUpdate();

            return productId;
        }

        psInsertProduct.setString(1, row.productName);
        psInsertProduct.setBigDecimal(2, row.salePrice);
        psInsertProduct.setString(3, row.categoryId);
        psInsertProduct.setString(4, row.supplierId);
        psInsertProduct.setString(5, DEFAULT_UNIT_ID);
        psInsertProduct.executeUpdate();

        try (ResultSet rsKeys = psInsertProduct.getGeneratedKeys()) {
            if (rsKeys.next()) {
                productId = rsKeys.getString(1);
            }
        }

        if (productId == null || productId.trim().isEmpty()) {
            psCheckProductAgain.setString(1, row.productName);

            try (ResultSet rs = psCheckProductAgain.executeQuery()) {
                if (rs.next()) {
                    productId = rs.getString("product_id");
                }
            }
        }

        return productId;
    }

    private void increaseInventory(
            String productId,
            int quantity,
            PreparedStatement psCheckInventory,
            PreparedStatement psUpdateInventory,
            PreparedStatement psInsertInventory
    ) throws SQLException {
        boolean exists = false;

        psCheckInventory.setString(1, productId);
        psCheckInventory.setString(2, DEFAULT_STORE_ID);

        try (ResultSet rs = psCheckInventory.executeQuery()) {
            exists = rs.next();
        }

        if (exists) {
            psUpdateInventory.setInt(1, quantity);
            psUpdateInventory.setString(2, productId);
            psUpdateInventory.setString(3, DEFAULT_STORE_ID);
            psUpdateInventory.executeUpdate();
        } else {
            psInsertInventory.setString(1, productId);
            psInsertInventory.setString(2, DEFAULT_STORE_ID);
            psInsertInventory.setInt(3, quantity);
            psInsertInventory.setString(4, DEFAULT_UNIT_NAME);
            psInsertInventory.executeUpdate();
        }
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
        String detailId = "PNDCSV" + System.nanoTime();

        ps.setString(1, detailId);
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
            BigDecimal lineTax,
            BigDecimal lineAfterTax,
            String createdBy,
            String fileName
    ) throws SQLException {
        String transactionId = "IVTCSV" + System.nanoTime();

        ps.setString(1, transactionId);
        ps.setString(2, receiptId);
        ps.setString(3, productId);
        ps.setInt(4, row.quantity);
        ps.setString(5, DEFAULT_UNIT_NAME);
        ps.setString(6, DEFAULT_STORE_ID);
        ps.setBigDecimal(7, row.importPriceBeforeVat);
        ps.setBigDecimal(8, row.salePrice);
        ps.setBigDecimal(9, row.vatRate);
        ps.setBigDecimal(10, lineTax);
        ps.setBigDecimal(11, lineAfterTax);
        ps.setString(12, "Nhập CSV từ file: " + fileName);
        ps.setString(13, createdBy);
        ps.executeUpdate();
    }

    private CsvProductRow parseCsvRow(String line) {
        String[] data = line.split(",", -1);

        if (data.length < 4) {
            throw new IllegalArgumentException("CSV phải có ít nhất 4 cột: product_name,sale_price,quantity,category_id");
        }

        CsvProductRow row = new CsvProductRow();

        row.productName = clean(data[0]);
        row.salePrice = new BigDecimal(clean(data[1]));
        row.quantity = Integer.parseInt(clean(data[2]));
        row.categoryId = clean(data[3]);

        if (row.categoryId.isEmpty()) {
            row.categoryId = "CAT001";
        }

        if (data.length >= 5 && !clean(data[4]).isEmpty()) {
            row.importPriceBeforeVat = new BigDecimal(clean(data[4]));
        }

        if (data.length >= 6 && !clean(data[5]).isEmpty()) {
            row.supplierId = clean(data[5]);
        } else {
            row.supplierId = DEFAULT_SUPPLIER_ID;
        }

        if (data.length >= 7 && !clean(data[6]).isEmpty()) {
            row.vatRate = new BigDecimal(clean(data[6]).replace("%", ""));
        }

        return row;
    }

    private BigDecimal resolveVatRateByCategory(String categoryId) {
        if (categoryId == null) {
            return BigDecimal.ZERO;
        }

        return switch (categoryId.trim().toUpperCase()) {
            case "CAT001" ->
                new BigDecimal("8");   // Thực phẩm khô
            case "CAT002" ->
                new BigDecimal("10");  // Đồ uống & Giải khát
            case "CAT003" ->
                new BigDecimal("10");  // Hàng tiêu dùng cá nhân
            case "CAT004" ->
                new BigDecimal("8");   // Bánh kẹo
            case "CAT005" ->
                new BigDecimal("5");   // Thực phẩm tươi sống
            default ->
                new BigDecimal("8");
        };
    }

    private boolean looksLikeHeader(String line) {
        String s = line == null ? "" : line.toLowerCase();

        return s.contains("product")
                || s.contains("tên")
                || s.contains("ten")
                || s.contains("sale_price")
                || s.contains("gia");
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

    private String getCurrentAccountId() {
        try {
            if (business.service.SessionManager.getCurrentUser() != null) {
                return business.service.SessionManager.getCurrentUser().getAccountId();
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
