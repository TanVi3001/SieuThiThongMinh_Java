package business.kpi;

import model.employee.EmployeePerformance;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser cho file CSV chứa dữ liệu KPI nhân viên SALES
 * Format file mong đợi:
 * Định danh NV (mã/phone/email/tên),Tên NV,Số Đơn,Doanh Thu,Tỷ Lệ Hoàn Thành (%),Tỷ Lệ Giao Hàng (%),Điểm Chuyên Cần
 * 
 * LƯU Ý: 
 * - Chỉ nhập các nhân viên SALES (bán hàng)
 * - Nhân viên chưa bán được đơn nào (Số Đơn = 0) sẽ được ghi nhận nhưng không được tính đạt KPI
 */
public class KpiCsvParser {

    public static List<EmployeePerformance> parseKpiFile(String filePath) throws IOException, IllegalArgumentException {
        List<EmployeePerformance> kpiList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNum = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                // Bỏ qua dòng tiêu đề (dòng đầu tiên)
                if (lineNum == 1) continue;
                
                // Bỏ qua dòng trống
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    EmployeePerformance ep = parseKpiLine(line);
                    if (ep != null) {
                        kpiList.add(ep);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        String.format("Lỗi tại dòng %d: %s. Chi tiết: %s", lineNum, line, e.getMessage())
                    );
                }
            }
        }
        
        if (kpiList.isEmpty()) {
            throw new IllegalArgumentException("File không chứa dữ liệu KPI hợp lệ");
        }
        
        return kpiList;
    }

    /**
     * Parse một dòng CSV thành EmployeePerformance
     * Hỗ trợ các định dạng:
     * - Với dấu phẩy: "0365099170,Tấn Tèo,50,10000000,95,98,8.5"
     * - Với dấu chấm phẩy: "0365099170;Tấn Tèo;50;10000000;95;98;8.5"
     * 
     * Nhân viên chưa bán được đơn nào (Số Đơn = 0) được chấp nhận nhưng sẽ không được tính đạt KPI
     */
    private static EmployeePerformance parseKpiLine(String line) throws Exception {
        String[] fields = line.split("[,;]");
        
        if (fields.length < 7) {
            throw new IllegalArgumentException(
                String.format("Cần ít nhất 7 cột. Tìm thấy %d cột", fields.length)
            );
        }
        
        EmployeePerformance ep = new EmployeePerformance();
        
        try {
            String identifier = fields[0].trim();
            String employeeName = fields[1].trim();
            
            // Resolve employee ID từ identifier (mã/phone/email/tên)
            String employeeId = KpiDataService.resolveEmployeeId(identifier, employeeName);
            if (employeeId == null) {
                System.err.println("⚠️ Cảnh báo: Không tìm thấy nhân viên SALES với định danh: " + identifier);
                return null; // Bỏ qua dòng này
            }
            
            ep.setEmployeeId(employeeId);
            ep.setEmployeeName(employeeName);
            ep.setTotalOrders(parseInt(fields[2].trim(), "Số Đơn"));
            ep.setRevenue(parseDouble(fields[3].trim(), "Doanh Thu"));
            ep.setCompletionRate(parseDouble(fields[4].trim(), "Tỷ Lệ Hoàn Thành"));
            ep.setDeliverySuccessRate(parseDouble(fields[5].trim(), "Tỷ Lệ Giao Hàng"));
            ep.setAttendanceScore(parseDouble(fields[6].trim(), "Điểm Chuyên Cần"));
            
            // Log cảnh báo nếu nhân viên chưa bán được đơn nào
            if (ep.getTotalOrders() == 0) {
                System.out.println("⚠️ " + employeeId + " chưa bán được đơn nào - sẽ không được tính đạt KPI");
            }
            
            // Tính toán điểm KPI
            ep.calculatePerformanceScore();
            
        } catch (NumberFormatException e) {
            throw new Exception("Định dạng số không hợp lệ: " + e.getMessage());
        }
        
        return ep;
    }

    private static int parseInt(String value, String fieldName) throws NumberFormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(fieldName + " phải là số nguyên, nhưng nhận được: " + value);
        }
    }

    private static double parseDouble(String value, String fieldName) throws NumberFormatException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(fieldName + " phải là số thực, nhưng nhận được: " + value);
        }
    }

    /**
     * Tạo file CSV mẫu để hướng dẫn người dùng
     */
    public static void createSampleCsvFile(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            writer.println("Mã NV,Tên NV,Số Đơn,Doanh Thu,Tỷ Lệ Hoàn Thành (%),Tỷ Lệ Giao Hàng (%),Điểm Chuyên Cần");
            writer.println("EMP001,Nguyễn Văn A,50,10000000,95,98,8.5");
            writer.println("EMP002,Trần Thị B,45,9500000,92,95,8.0");
            writer.println("EMP003,Phạm Văn C,55,11000000,98,99,9.0");
            writer.println("EMP004,Lê Thị D,40,8000000,88,90,7.5");
            writer.println("EMP005,Hoàng Văn E,60,12500000,99,100,9.5");
        }
    }
}
