package model.order;

/**
 * Model OrderDetail
 */
public class OrderDetail {

    private String orderDetailId;
    private String orderId;
    private String productId;
    private int quantity;
    private double unitPrice;
    private String unitId;
    private int quantityInBaseUnit;
    private boolean isDeleted;

    public OrderDetail() {
        this.isDeleted = false;
    }

    public OrderDetail(String orderId, String productId, int quantity, double unitPrice) {
        this(orderId, productId, quantity, unitPrice, null, 0);
    }

    public OrderDetail(
            String orderId,
            String productId,
            int quantity,
            double unitPrice,
            String unitId,
            int quantityInBaseUnit
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitId = unitId;
        this.quantityInBaseUnit = quantityInBaseUnit;
        this.isDeleted = false;
    }

    public String getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(String orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public int getQuantityInBaseUnit() {
        return quantityInBaseUnit > 0 ? quantityInBaseUnit : quantity;
    }

    public void setQuantityInBaseUnit(int quantityInBaseUnit) {
        this.quantityInBaseUnit = quantityInBaseUnit;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    public double getLineTotal() {
        return quantity * unitPrice;
    }

    @Override
    public String toString() {
        return "OrderDetail{"
                + "orderDetailId='" + orderDetailId + '\''
                + ", orderId='" + orderId + '\''
                + ", productId='" + productId + '\''
                + ", quantity=" + quantity
                + ", unitPrice=" + unitPrice
                + ", unitId='" + unitId + '\''
                + ", quantityInBaseUnit=" + quantityInBaseUnit
                + ", isDeleted=" + isDeleted
                + '}';
    }
}
