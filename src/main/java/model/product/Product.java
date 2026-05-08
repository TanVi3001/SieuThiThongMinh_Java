package model.product;

import java.math.BigDecimal;

/**
 * @author nguye - Fixed by Leader Vi
 */
public class Product {

    private String productId;
    private String productName;
    private BigDecimal basePrice;
    private String categoryId;
    private String supplierId;
    private String baseUnitId;
    private int isDeleted;
    private int quantity;

    // THÊM MỚI để khớp INVENTORY
    private String storeId;
    private String unit;

    public Product() {
    }

    // Constructor cũ
    public Product(String productId, String productName, BigDecimal basePrice,
            String categoryId, String supplierId, int isDeleted, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        this.isDeleted = isDeleted;
        this.quantity = quantity;
    }

    // Constructor đầy đủ hơn (khuyên dùng)
    public Product(String productId, String productName, BigDecimal basePrice,
            String categoryId, String supplierId, int isDeleted, int quantity,
            String storeId, String unit) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        this.isDeleted = isDeleted;
        this.quantity = quantity;
        this.storeId = storeId;
        this.unit = unit;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getBaseUnitId() {
        return baseUnitId;
    }

    public void setBaseUnitId(String baseUnitId) {
        this.baseUnitId = baseUnitId;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // ===== SỬA CHÍNH Ở ĐÂY =====
    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
    // Thêm vào file Product.java
    private String unitId;

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }
}
