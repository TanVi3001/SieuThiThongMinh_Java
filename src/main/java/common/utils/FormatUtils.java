package common.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    // Hàm biến 10000 -> 10.000 đ cho Quỳnh hiển thị lên bảng (dùng BigDecimal)
    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat vn = NumberFormat.getCurrencyInstance(localeVN);
        return vn.format(amount);
    }

    // BỔ SUNG: Hàm format tiền tệ tương tự nhưng nhận đầu vào là double 
    // (Dùng cho bên module EmployeePerformance - KPI Dashboard)
    public static String formatCurrency(double amount) {
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat vn = NumberFormat.getCurrencyInstance(localeVN);
        return vn.format(amount);
    }

    // Hàm check xem user có nhập "láo" (nhập chữ vào ô số lượng) không
    public static boolean isNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // BỔ SUNG: Định dạng số lượng hoặc số thập phân cơ bản (có dấu phẩy ngăn cách hàng nghìn)
    // Ví dụ: 1500000.5 -> "1,500,000.5"
    public static String formatNumber(double number) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        return numberFormat.format(number);
    }

    // BỔ SUNG: Định dạng ngày tháng sang chuỗi chuẩn dd/MM/yyyy
    // Ví dụ: 2026-05-08 -> "08/05/2026"
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }
}
