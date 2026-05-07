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
        return statisticSql.getRevenueReport(fromDate, toDate);
    }

    public List<Object[]> getProductReport(Date fromDate, Date toDate) {
        validateDateRange(fromDate, toDate);
        return statisticSql.getProductReport(fromDate, toDate);
    }

    public List<Object[]> getEmployeeReport(Date fromDate, Date toDate) {
        validateDateRange(fromDate, toDate);
        return statisticSql.getEmployeeReport(fromDate, toDate);
    }

    private void validateDateRange(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ Từ ngày và Đến ngày.");
        }
        if (fromDate.after(toDate)) {
            throw new IllegalArgumentException("Từ ngày không được lớn hơn Đến ngày.");
        }
    }
}
