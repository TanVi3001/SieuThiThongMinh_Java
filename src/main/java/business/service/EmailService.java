package business.service;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {
    // TÙNG NHỚ THAY EMAIL VÀ APP PASSWORD CỦA BẠN VÀO ĐÂY NHÉ
    private static final String MY_EMAIL = "papylovevip1@gmail.com"; 
    private static final String APP_PASSWORD = "hzvpraxtcxhkynkg"; 

    public static boolean sendActivationEmail(String toEmail, String employeeName, String activationCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MY_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(MY_EMAIL, "Smart Supermarket System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("THÔNG BÁO TÀI KHOẢN KÍCH HOẠT NHÂN VIÊN MỚI");

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

            message.setContent(htmlContent, "text/html; charset=UTF-8");
            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}