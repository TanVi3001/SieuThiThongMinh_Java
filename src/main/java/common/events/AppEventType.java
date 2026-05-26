package common.events;

/**
 * Các nhóm dữ liệu/sự kiện realtime trong hệ thống.
 *
 * Giữ lại tên cũ để không phá code hiện tại, đồng thời bổ sung tên mới rõ nghĩa
 * hơn.
 */
public enum AppEventType {
    UNKNOWN,
    // Tên mới, rõ module
    PRODUCT_CHANGED,
    INVENTORY_CHANGED,
    SUPPLIER_CHANGED,
    CUSTOMER_CHANGED,
    EMPLOYEE_CHANGED,
    SHIFT_CHANGED,
    ORDER_CHANGED,
    PROMOTION_CHANGED,
    STORE_CHANGED,
    ACCOUNT_CHANGED,
    ROLE_CHANGED,
    DASHBOARD_CHANGED,
    REPORT_CHANGED,
    SYSTEM_CONFIG_CHANGED,
    // Tên cũ để tương thích code hiện tại
    SYSTEM_CONFIG,
    ACCOUNT_SECURITY,
    STORE_INFO,
    PRODUCTS,
    INVENTORY,
    INVENTORY_ALERT,
    CUSTOMERS,
    EMPLOYEES,
    ORDERS,
    STATISTICS,
    DASHBOARD
}
