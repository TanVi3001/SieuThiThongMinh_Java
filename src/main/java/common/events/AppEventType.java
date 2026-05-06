package common.events;

/**
 * Các nhóm dữ liệu cần đồng bộ.
 * Bạn có thể thêm: PRODUCTS, ORDERS, CUSTOMERS...
 */
public enum AppEventType {
    SYSTEM_CONFIG,
    ACCOUNT_SECURITY,
    PRODUCTS,
    INVENTORY,
    UNKNOWN, EMPLOYEES, CUSTOMERS,ORDERS
}