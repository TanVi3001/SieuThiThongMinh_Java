package model.order;

/**
 * Model OrderDetail Leader: Lê Tấn Vĩ (Đã dọn dẹp đống rác của Tùng)
 */
public class OrderDetail {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final double unitPrice;
    private final String unitId;
    private final int quantityInBaseUnit;
    private boolean isDeleted;

    public OrderDetail(String orderId, String productId, int quantity, double unitPrice) {
        this(orderId, productId, quantity, unitPrice, null, 0);
    }

    public OrderDetail(String orderId, String productId, int quantity, double unitPrice,
            String unitId, int quantityInBaseUnit) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitId = unitId;
        this.quantityInBaseUnit = quantityInBaseUnit;
        this.isDeleted = false;
    }
    // Thêm vào file OrderDetail.java

    // Sửa lại để trả về giá trị thực tế thay vì ném lỗi
    public String getProductId() {
        return productId;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getUnitId() {
        return unitId;
    }

    public int getQuantityInBaseUnit() {
        return quantityInBaseUnit > 0 ? quantityInBaseUnit : quantity;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    //Setter
    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

   
}
