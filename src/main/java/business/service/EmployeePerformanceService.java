package business;

import business.hr_kpi.EmployeePerformanceSql;
import model.employee.EmployeePerformance;
import java.util.Comparator;
import java.util.List;

public class EmployeePerformanceService {

    private final EmployeePerformanceSql kpiSql;

    public EmployeePerformanceService() {
        this.kpiSql = new EmployeePerformanceSql();
    }

    // Lấy toàn bộ dữ liệu thống kê từ SQL
    public List<EmployeePerformance> getDashboardData() {
        return kpiSql.getAllEmployeeKPIs();
    }

    // 1. Tìm nhân viên có doanh thu cao nhất (Top Sale)
    public EmployeePerformance getTopSaleEmployee(List<EmployeePerformance> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.stream()
                .max(Comparator.comparingDouble(EmployeePerformance::getRevenue))
                .orElse(null);
    }

    // 2. Tìm nhân viên giao hàng xuất sắc nhất (Best Delivery)
    public EmployeePerformance getBestDeliveryEmployee(List<EmployeePerformance> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.stream()
                .filter(e -> e.getTotalDeliveries() > 0) // Chỉ xét những người có tham gia giao hàng
                .max(Comparator.comparingDouble(EmployeePerformance::getDeliverySuccessRate)
                        // Nếu tỷ lệ thành công bằng nhau, ưu tiên người giao nhiều đơn hơn
                        .thenComparingInt(EmployeePerformance::getTotalDeliveries))
                .orElse(null);
    }

    // 3. Tìm nhân viên xuất sắc nhất tháng (Top KPI)
    public EmployeePerformance getTopKpiEmployee(List<EmployeePerformance> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.stream()
                .max(Comparator.comparingDouble(EmployeePerformance::getPerformanceScore))
                .orElse(null);
    }

    // 4. Tìm nhân viên chuyên cần nhất (Best Attendance)
    public EmployeePerformance getBestAttendanceEmployee(List<EmployeePerformance> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.stream()
                .filter(e -> e.getTotalWorkDays() > 0) // Chỉ xét người có đi làm
                .max(Comparator.comparingDouble(EmployeePerformance::getAttendanceScore)
                        // Nếu điểm chuyên cần bằng nhau, ưu tiên người có số ngày làm việc nhiều hơn
                        .thenComparingInt(EmployeePerformance::getTotalWorkDays))
                .orElse(null);
    }
}
