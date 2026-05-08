package model.order;

import java.sql.Date;

public class Order {

    private String orderId;
    private String customerId;
    private String employeeId;
    private String paymentMethodId;

    // =====================================================
    // BỔ SUNG MỚI
    // =====================================================
    private String employeeName;

    private Date orderDate;
    private double totalAmount;
    private String status;
    private String note;
    private boolean isDeleted;

    public Order() {
    }

    /**
     * Constructor đầy đủ 10 tham số
     */
    public Order(String orderId,
            String customerId,
            String employeeId,
            String paymentMethodId,
            String employeeName,
            Date orderDate,
            double totalAmount,
            String status,
            String note,
            boolean isDeleted) {

        this.orderId = orderId;
        this.customerId = customerId;
        this.employeeId = employeeId;
        this.paymentMethodId = paymentMethodId;
        this.employeeName = employeeName;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.note = note;
        this.isDeleted = isDeleted;
    }

    /**
     * Constructor POS
     */
    public Order(String orderId,
            String customerId,
            String employeeId,
            String paymentMethodId,
            Date orderDate,
            double totalAmount,
            String status,
            String note,
            boolean isDeleted) {

        this(orderId,
                customerId,
                employeeId,
                paymentMethodId,
                null,
                orderDate,
                totalAmount,
                status,
                note,
                isDeleted);
    }

    /**
     * Constructor tương thích ngược
     */
    public Order(String orderId,
            String customerId,
            String employeeId,
            Date orderDate,
            double totalAmount,
            String status,
            boolean isDeleted) {

        this(orderId,
                customerId,
                employeeId,
                null,
                null,
                orderDate,
                totalAmount,
                status,
                null,
                isDeleted);
    }

    /**
     * Constructor tương thích ngược
     */
    public Order(String id,
            String cusId,
            String empId,
            Date date,
            double total,
            String note) {

        this(id,
                cusId,
                empId,
                null,
                null,
                date,
                total,
                note,
                null,
                false);
    }

    // =====================================================
    // GETTERS
    // =====================================================
    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    // =====================================================
    // SETTERS
    // =====================================================
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setPaymentMethodId(String pmId) {
        this.paymentMethodId = pmId;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}
