package business.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.function.Consumer;
import common.db.DatabaseConnection;

public class ProductImportService {

    public void importProductCSV(String filePath, Consumer<Integer> progressCallback) {
        String checkProductSql = "SELECT product_id FROM PRODUCTS WHERE product_name = ?";
        String insertProductSql = "INSERT INTO PRODUCTS (product_name, base_price, category_id, supplier_id, base_unit_id) VALUES (?, ?, ?, ?, ?)";

        // Đã sửa: Bảng INVENTORY có khóa chính kép (product_id, store_id) nên phải truy vấn theo cả 2
        String checkInventorySql = "SELECT quantity FROM INVENTORY WHERE product_id = ? AND store_id = ?";
        String updateInventorySql = "UPDATE INVENTORY SET quantity = quantity + ? WHERE product_id = ? AND store_id = ?";
        String insertInventorySql = "INSERT INTO INVENTORY (product_id, store_id, quantity) VALUES (?, ?, ?)";

        // Dữ liệu mặc định đã có sẵn trong seed_data.sql
        String defaultStoreId = "ST01";
        String defaultSupplierId = "SUP_01";
        String defaultUnitId = "UN_01";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement psCheckProd = conn.prepareStatement(checkProductSql); // Khai báo rõ PRODUCT_ID để getGeneratedKeys hoạt động tốt với Oracle Trigger
                 PreparedStatement psInsertProd = conn.prepareStatement(insertProductSql, new String[]{"PRODUCT_ID"}); PreparedStatement psCheckInv = conn.prepareStatement(checkInventorySql); PreparedStatement psUpdateInv = conn.prepareStatement(updateInventorySql); PreparedStatement psInsertInv = conn.prepareStatement(insertInventorySql); BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            if (conn == null) {
                throw new SQLException("Không thể kết nối Database!");
            }

            // Tắt auto-commit để đảm bảo tính toàn vẹn dữ liệu
            conn.setAutoCommit(false);

            String line;
            boolean isFirstLine = true;
            int count = 0;

            while ((line = br.readLine()) != null) {
                // Khử ký tự ẩn BOM
                if (isFirstLine) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",");
                if (data.length >= 4) {
                    try {
                        String productName = data[0].trim();
                        double basePrice = Double.parseDouble(data[1].trim());
                        int quantity = Integer.parseInt(data[2].trim());
                        String categoryId = data[3].trim();

                        // Phòng hờ CSV bị thiếu ID Danh mục, ép về CAT001 mặc định để tránh lỗi khóa ngoại
                        if (categoryId.isEmpty()) {
                            categoryId = "CAT001";
                        }

                        String productId = null;

                        // 1. Kiểm tra sản phẩm đã tồn tại chưa
                        psCheckProd.setString(1, productName);
                        try (ResultSet rsProd = psCheckProd.executeQuery()) {
                            if (rsProd.next()) {
                                productId = rsProd.getString("product_id");
                            }
                        }

                        // 2. Thêm mới nếu chưa có
                        if (productId == null) {
                            psInsertProd.setString(1, productName);
                            psInsertProd.setDouble(2, basePrice);
                            psInsertProd.setString(3, categoryId);
                            psInsertProd.setString(4, defaultSupplierId);
                            psInsertProd.setString(5, defaultUnitId);
                            psInsertProd.executeUpdate();

                            // Query lại để lấy ID vừa được trigger sinh ra
                            psCheckProd.setString(1, productName);
                            try (ResultSet rsProd2 = psCheckProd.executeQuery()) {
                                if (rsProd2.next()) {
                                    productId = rsProd2.getString("product_id");
                                }
                            }
                        }

                        // 3. Xử lý Kho (Kiểm tra theo CẢ product_id VÀ store_id)
                        if (productId != null) {
                            psCheckInv.setString(1, productId);
                            psCheckInv.setString(2, defaultStoreId);
                            boolean existsInInv = false;

                            try (ResultSet rsInv = psCheckInv.executeQuery()) {
                                if (rsInv.next()) {
                                    existsInInv = true;
                                }
                            }

                            if (existsInInv) {
                                psUpdateInv.setInt(1, quantity);
                                psUpdateInv.setString(2, productId);
                                psUpdateInv.setString(3, defaultStoreId);
                                psUpdateInv.executeUpdate();
                            } else {
                                psInsertInv.setString(1, productId);
                                psInsertInv.setString(2, defaultStoreId);
                                psInsertInv.setInt(3, quantity);
                                psInsertInv.executeUpdate();
                            }
                        }

                        count++;
                        if (progressCallback != null) {
                            // Giả định mẫu số tạm thời là 1000 dòng để callback, bạn có thể truyền tổng số dòng thật vào sau
                            progressCallback.accept(Math.min(99, (count * 100) / 1000));
                        }

                    } catch (NumberFormatException e) {
                        System.err.println("❌ Lỗi định dạng số tại dòng: " + line);
                    } catch (SQLIntegrityConstraintViolationException e) {
                        System.err.println("❌ Lỗi Khóa Ngoại tại dòng (Khả năng cao mã danh mục không tồn tại trong CSDL): " + line);
                        System.err.println("   Chi tiết: " + e.getMessage());
                    } catch (SQLException e) {
                        System.err.println("❌ Lỗi SQL tại dòng: " + line);
                        System.err.println("   Chi tiết: " + e.getMessage());
                    }
                }
            }

            // Hoàn tất lưu xuống database
            conn.commit();
            if (progressCallback != null) {
                progressCallback.accept(100);
            }
            System.out.println("✅ Import hoàn tất: " + count + " sản phẩm.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi nghiêm trọng khi import CSV: " + e.getMessage());
        }
    }
}
