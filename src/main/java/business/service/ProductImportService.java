package business.service;

import business.sql.prod_inventory.ProductsSql;
import common.db.DatabaseConnection;
import common.realtime.RealtimeNotifier;

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
import java.util.ArrayList;
import java.util.List;
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
     * lỗi. Bên trong chạy flow mới: import CSV + tạo phiếu nhập.
     */
    public void importProductCSV(String filePath, Consumer<Integer> progressCallback) {
        importProductCSVWithReceipt(filePath, progressCallback);
    }

    /**
     * Hỗ trợ các format CSV:
     *
     * 1) Format cũ: product_name,base_price,quantity,category_id
     *
     * 2) Format có ảnh: product_name,base_price,quantity,category_id,image_path
     *
     * 3) Format đầy đủ:
     * product_name,base_price,quantity,category_id,image_path,import_price,supplier_id,vat_rate
     *
     * image_path nên lưu dạng tương đối: products/mi_hao_hao_tom_chua_cay.png
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

        /*
         * YÊU CẦU DB:
         * ALTER TABLE PRODUCTS ADD image_path VARCHAR2(255);
         */
        String sqlInsertProduct = """
            INSERT INTO PRODUCTS (
                product_id,
                product_name,
                base_price,
                category_id,
                supplier_id,
                base_unit_id,
                image_path,
                is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)
        """;

        /*
         * Fix lỗi chính:
         * File cũ chỉ UPDATE base_price/category/supplier nhưng code lại set thêm imagePath.
         * Điều đó làm import bị lỗi từng dòng và thành công = 0.
         */
        String sqlUpdateProduct = """
            UPDATE PRODUCTS
            SET base_price = ?,
                category_id = ?,
                supplier_id = ?,
                image_path = CASE
                    WHEN ? IS NULL OR TRIM(?) = '' THEN image_path
                    ELSE ?
                END
            WHERE product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        String sqlUpsertInventory = """
            MERGE INTO INVENTORY inv
            USING (
                SELECT ? AS product_id,
                       ? AS store_id,
                       ? AS quantity,
                       ? AS unit
                FROM dual
            ) src
            ON (
                inv.product_id = src.product_id
                AND inv.store_id = src.store_id
            )
            WHEN MATCHED THEN
                UPDATE SET
                    inv.quantity = NVL(inv.quantity, 0) + src.quantity,
                    inv.unit = src.unit,
                    inv.last_updated = SYSDATE,
                    inv.is_deleted = 0
            WHEN NOT MATCHED THEN
                INSERT (
                    product_id,
                    store_id,
                    quantity,
                    unit,
                    last_updated,
                    is_deleted
                )
                VALUES (
                    src.product_id,
                    src.store_id,
                    src.quantity,
                    src.unit,
                    SYSDATE,
                    0
                )
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
                    PreparedStatement psInsertReceipt = conn.prepareStatement(sqlInsertReceipt); PreparedStatement psUpdateReceiptTotal = conn.prepareStatement(sqlUpdateReceiptTotal); PreparedStatement psCheckProduct = conn.prepareStatement(sqlCheckProduct); PreparedStatement psInsertProduct = conn.prepareStatement(sqlInsertProduct); PreparedStatement psUpdateProduct = conn.prepareStatement(sqlUpdateProduct); PreparedStatement psUpsertInventory = conn.prepareStatement(sqlUpsertInventory); PreparedStatement psUpsertStoreProduct = conn.prepareStatement(sqlUpsertStoreProduct); PreparedStatement psInsertDetail = conn.prepareStatement(sqlInsertDetail); PreparedStatement psInsertTransaction = conn.prepareStatement(sqlInsertTransaction)) {
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
                            System.err.println("[IMPORT CSV] Bỏ qua dòng không hợp lệ: " + line);
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
                            System.err.println("[IMPORT CSV] Bỏ qua vì sai logic giá: " + row.productName);
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
                            System.err.println("[IMPORT CSV] Không lấy được product_id cho dòng: " + line);
                            continue;
                        }

                        upsertInventory(
                                productId,
                                row.quantity,
                                currentStoreId,
                                psUpsertInventory
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
                        System.err.println("[IMPORT CSV] Lỗi xử lý dòng: " + line);
                        rowEx.printStackTrace();
                    }
                }

                psUpdateReceiptTotal.setBigDecimal(1, result.totalBeforeTax);
                psUpdateReceiptTotal.setBigDecimal(2, result.totalTax);
                psUpdateReceiptTotal.setBigDecimal(3, result.totalAfterTax);
                psUpdateReceiptTotal.setString(4, result.receiptId);
                psUpdateReceiptTotal.executeUpdate();

                conn.commit();

                RealtimeNotifier.inventoryChanged("PURCHASE_RECEIPT_CREATED:" + result.receiptId + ":STORE:" + currentStoreId);
                RealtimeNotifier.productsChanged("PRODUCT_STOCK_INCREASED_BY_RECEIPT:" + result.receiptId + ":STORE:" + currentStoreId);
                RealtimeNotifier.statisticsChanged("PURCHASE_RECEIPT_CREATED:" + result.receiptId + ":STORE:" + currentStoreId);

                if (progressCallback != null) {
                    progressCallback.accept(100);
                }

                System.out.println("[IMPORT CSV] Hoàn tất.");
                System.out.println("[IMPORT CSV] Chi nhánh nhập: " + currentStoreId);
                System.out.println("[IMPORT CSV] Mã phiếu nhập: " + result.receiptId);
                System.out.println("[IMPORT CSV] Tổng dòng: " + result.totalRows);
                System.out.println("[IMPORT CSV] Thành công: " + result.successRows);
                System.out.println("[IMPORT CSV] Bỏ qua: " + result.skippedRows);

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
            psUpdateProduct.setString(4, row.imagePath);
            psUpdateProduct.setString(5, row.imagePath);
            psUpdateProduct.setString(6, row.imagePath);
            psUpdateProduct.setString(7, productId);
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
        psInsertProduct.setString(7, row.imagePath);
        psInsertProduct.executeUpdate();

        return productId;
    }

    private void upsertInventory(
            String productId,
            int quantity,
            String storeId,
            PreparedStatement psUpsertInventory
    ) throws SQLException {
        psUpsertInventory.setString(1, productId);
        psUpsertInventory.setString(2, storeId);
        psUpsertInventory.setInt(3, quantity);
        psUpsertInventory.setString(4, DEFAULT_UNIT_NAME);
        psUpsertInventory.executeUpdate();
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
        List<String> data = splitCsvLine(line);

        if (data.size() < 4) {
            throw new IllegalArgumentException(
                    "CSV phải có ít nhất 4 cột: product_name,base_price,quantity,category_id"
            );
        }

        CsvProductRow row = new CsvProductRow();

        row.productName = clean(data.get(0));
        row.salePrice = parseMoney(clean(data.get(1)));
        row.quantity = Integer.parseInt(clean(data.get(2)));

        String categoryId = clean(data.get(3));
        row.categoryId = categoryId.isEmpty() ? "CAT001" : categoryId;

        /*
         * data[4] có thể là image_path hoặc import_price.
         * Ví dụ:
         * products/mi_hao_hao_tom_chua_cay.png
         * hoặc
         * 3500
         */
        if (data.size() >= 5) {
            String col5 = clean(data.get(4));

            if (!col5.isEmpty()) {
                if (looksLikeImagePath(col5)) {
                    row.imagePath = normalizeImagePath(col5);
                } else {
                    row.importPriceBeforeVat = parseMoney(col5);
                }
            }
        }

        if (data.size() >= 6) {
            String col6 = clean(data.get(5));

            if (!col6.isEmpty()) {
                if (looksLikeImagePath(col6)) {
                    row.imagePath = normalizeImagePath(col6);
                } else if (looksLikeSupplierId(col6)) {
                    row.supplierId = col6;
                } else {
                    row.importPriceBeforeVat = parseMoney(col6);
                }
            }
        }

        if (data.size() >= 7) {
            String col7 = clean(data.get(6));

            if (!col7.isEmpty()) {
                if (looksLikeSupplierId(col7)) {
                    row.supplierId = col7;
                } else {
                    row.vatRate = parseMoney(col7.replace("%", ""));
                }
            }
        }

        if (data.size() >= 8) {
            String col8 = clean(data.get(7));

            if (!col8.isEmpty()) {
                row.vatRate = parseMoney(col8.replace("%", ""));
            }
        }

        if (row.supplierId == null || row.supplierId.trim().isEmpty()) {
            row.supplierId = DEFAULT_SUPPLIER_ID;
        }

        return row;
    }

    /**
     * Parser CSV đơn giản nhưng có hỗ trợ dấu ngoặc kép. Tránh lỗi nếu tên sản
     * phẩm có dấu phẩy trong tương lai.
     */
    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();

        if (line == null) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        result.add(current.toString());
        return result;
    }

    private boolean looksLikeImagePath(String value) {
        if (value == null) {
            return false;
        }

        String s = value.trim().toLowerCase();

        return s.endsWith(".png")
                || s.endsWith(".jpg")
                || s.endsWith(".jpeg")
                || s.endsWith(".gif")
                || s.startsWith("products/")
                || s.contains("/products/")
                || s.contains("view/image/");
    }

    private String normalizeImagePath(String imagePath) {
        if (imagePath == null) {
            return null;
        }

        String s = imagePath.trim()
                .replace("\\", "/")
                .replace("\"", "");

        if (s.isEmpty()) {
            return null;
        }

        /*
         * Nếu CSV chỉ ghi ten_file.png thì tự đưa vào thư mục products/.
         */
        if (!s.contains("/")) {
            s = "products/" + s;
        }

        /*
         * Nếu CSV ghi:
         * src/main/resources/view/image/products/a.png
         * thì cắt còn:
         * products/a.png
         */
        String marker = "view/image/";
        int idx = s.indexOf(marker);
        if (idx >= 0) {
            s = s.substring(idx + marker.length());
        }

        return s;
    }

    private boolean looksLikeSupplierId(String value) {
        if (value == null) {
            return false;
        }

        String s = value.trim().toUpperCase();
        return s.startsWith("SUP");
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
                || s.contains("price")
                || s.contains("image_path");
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
        String imagePath;
        BigDecimal importPriceBeforeVat;
        String supplierId = DEFAULT_SUPPLIER_ID;
        BigDecimal vatRate;
    }
}