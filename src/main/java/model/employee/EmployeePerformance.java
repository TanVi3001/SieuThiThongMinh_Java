package model.employee;

public class EmployeePerformance {

    private String employeeId;
    private String employeeName;

    // Chỉ số Bán hàng
    private int totalOrders;
    private double revenue;
    private double avgOrderValue;
    private double completionRate;

    // Chỉ số Giao hàng
    private int totalDeliveries;
    private double deliverySuccessRate;

    // Chỉ số Chuyên cần
    private int totalWorkDays;
    private double attendanceScore;

    // Điểm tổng hợp
    private double performanceScore;

    public EmployeePerformance() {
    }

    // Hàm tự động tính điểm KPI dựa trên trọng số bạn yêu cầu
    public void calculatePerformanceScore() {
        // Công thức: Doanh thu(chuẩn hóa) + Số đơn(chuẩn hóa) + Tỷ lệ HT(20%) + Chuyên cần(20%)
        // Lưu ý: Doanh thu và Số đơn nên được chuẩn hóa/chia tỷ lệ (scale) so với target trước khi nhân trọng số. 
        // Tạm thời áp dụng công thức rút gọn dựa trên các chỉ số % đã có:
        this.performanceScore = (this.completionRate * 0.4)
                + (this.deliverySuccessRate * 0.3)
                + (this.attendanceScore * 10 * 0.3); // Giả sử attendanceScore max là 10
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public double getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(double avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public double getDeliverySuccessRate() {
        return deliverySuccessRate;
    }

    public void setDeliverySuccessRate(double deliverySuccessRate) {
        this.deliverySuccessRate = deliverySuccessRate;
    }

    public int getTotalWorkDays() {
        return totalWorkDays;
    }

    public void setTotalWorkDays(int totalWorkDays) {
        this.totalWorkDays = totalWorkDays;
    }

    public double getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(double attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }
}
