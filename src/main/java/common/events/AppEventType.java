package common.events;

/**
 * Các nhóm dữ liệu/sự kiện cần đồng bộ real-time trong hệ thống.
 */
public enum AppEventType {
    UNKNOWN,
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
