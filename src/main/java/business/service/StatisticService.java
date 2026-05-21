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

    private String currentScopedStoreId() {
        if (SessionManager.isAdmin()) {
            return null;
        }
        String storeId = SessionManager.getCurrentStoreId();
        return storeId == null || storeId.trim().isEmpty() ? null : storeId.trim();
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
