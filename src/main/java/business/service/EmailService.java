package business.service;

import common.utils.EmailUtils;

public class EmailService {

    // =========================================================================
    // CẤU HÌNH TÀI KHOẢN GỬI MAIL (CHỈ CẦN SỬA Ở ĐÂY NẾU THAY ĐỔI)
    // =========================================================================
    private static final String MY_EMAIL = "nguyentung28012006@gmail.com"; 
    private static final String APP_PASSWORD = "yrmx mviw enuj ydce"; 

    /**
     * 1. HÀM DÙNG CHO: Cấp tài khoản nhân viên / Đăng ký
     */
    public static boolean sendActivationEmail(String toEmail, String employeeName, String activationCode) {
        try {
            String subject = "THÔNG BÁO TÀI KHOẢN KÍCH HOẠT NHÂN VIÊN MỚI";
            // Template Email xịn xò
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px; max-width: 500px; margin: auto;'>"
                    + "<h2 style='color: #365CF5; text-align: center;'>Chào mừng gia nhập Smart Supermarket!</h2>"
                    + "<p>Xin chào <b>" + employeeName + "</b>,</p>"
                    + "<p>Hồ sơ nhân sự của bạn đã được Quản lý khởi tạo thành công trên hệ thống. Để bắt đầu làm việc, vui lòng đăng ký tài khoản bằng Mã kích hoạt dưới đây:</p>"
                    + "<div style='background-color: #F4F6FA; padding: 15px; text-align: center; border-radius: 8px; margin: 20px 0;'>"
                    + "<h1 style='color: #DC3545; margin: 0; letter-spacing: 2px;'>" + activationCode + "</h1>"
                    + "</div>"
                    + "<p><b>Hướng dẫn:</b><br/>1. Mở ứng dụng Smart Supermarket<br/>2. Chọn <i>Đăng ký</i><br/>3. Nhập Mã kích hoạt trên để thiết lập tên đăng nhập và mật khẩu của riêng bạn.</p>"
                    + "<hr style='border: top 1px solid #eee;'/>"
                    + "<p style='font-size: 12px; color: #888; text-align: center;'>Đây là email tự động, vui lòng không trả lời.</p>"
                    + "</div>";

            EmailUtils.sendEmail(MY_EMAIL, APP_PASSWORD, toEmail, subject, htmlContent);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 2. HÀM DÙNG CHO: Quên mật khẩu (OTP)
     */
    public static boolean sendPasswordRecoveryOTP(String targetEmail, String username, String otp) {
        try {
            String subject = "Khôi phục tài khoản (Quên mật khẩu) - Smart Supermarket";
            String content = "Chào bạn,\n\n"
                    + "Tên đăng nhập của bạn là: " + username + "\n"
                    + "Mã xác minh (OTP) để đổi mật khẩu là: " + otp + "\n\n"
                    + "Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ cho bất kỳ ai để bảo vệ tài khoản!";

            EmailUtils.sendEmail(MY_EMAIL, APP_PASSWORD, targetEmail, subject, content);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail OTP khôi phục: " + e.getMessage());
            return false;
        }
    }
}