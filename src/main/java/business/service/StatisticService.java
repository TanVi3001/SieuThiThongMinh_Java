package business.service;

import business.sql.sales_order.StatisticSql;
import java.util.Date;
import java.util.List;

public class StatisticService {

    private final StatisticSql statisticSql;

    public StatisticService() {
        this.statisticSql = StatisticSql.getInstance();
    }

    public List<Object[]> getRevenueReport(Date fromDate, Date toDate) {
        validateDateRange(fromDate, toDate);

        String storeId = currentScopedStoreId();

        if (storeId != null) {
            return statisticSql.getRevenueReportByStore(fromDate, toDate, storeId);
        }

        return statisticSql.getRevenueReport(fromDate, toDate);
    }

    public List<Object[]> getProductReport(Date fromDate, Date toDate) {
        validateDateRange(fromDate, toDate);

        String storeId = currentScopedStoreId();

        if (storeId != null) {
            return statisticSql.getProductReportByStore(fromDate, toDate, storeId);
        }

        return statisticSql.getProductReport(fromDate, toDate);
    }

    public List<Object[]> getEmployeeReport(Date fromDate, Date toDate) {
        validateDateRange(fromDate, toDate);

        String storeId = currentScopedStoreId();

        if (storeId != null) {
            return statisticSql.getEmployeeReportByStore(fromDate, toDate, storeId);
        }

        return statisticSql.getEmployeeReport(fromDate, toDate);
    }

    public boolean isScopedByStore() {
        return currentScopedStoreId() != null;
    }

    public String getCurrentReportStoreId() {
        return currentScopedStoreId();
    }

    public String getCurrentReportStoreName() {
        if (SessionManager.isAdmin()) {
            return "Toàn hệ thống";
        }

        String storeName = SessionManager.getCurrentStoreName();

        if (storeName != null && !storeName.trim().isEmpty()) {
            return storeName.trim();
        }

        String storeId = SessionManager.getCurrentStoreId();

        if (storeId != null && !storeId.trim().isEmpty()) {
            return storeId.trim();
        }

        return "Chưa xác định chi nhánh";
    }

    private String currentScopedStoreId() {
        if (SessionManager.isAdmin()) {
            return null;
        }

        String storeId = SessionManager.getCurrentStoreId();

        if (storeId == null || storeId.trim().isEmpty()) {
            return null;
        }

        return storeId.trim();
    }

    private void validateDateRange(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ Từ ngày và Đến ngày.");
        }

        if (fromDate.after(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }
    }
}
