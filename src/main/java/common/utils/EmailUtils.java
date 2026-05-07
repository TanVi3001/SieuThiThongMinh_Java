package common.utils;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 *
 * @author nguye
 */
public class EmailUtils {
    /**
     * Hàm gửi mail linh hoạt với tài khoản người dùng tự nhập
     */
    public static void sendEmail(String fromEmail, String appPassword, String toEmail, String subject, String body) 
            throws MessagingException {
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Đăng nhập bằng tài khoản vừa nhập trên giao diện
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        
        // SỬA Ở ĐÂY: Set Content Type là HTML thay vì Text thông thường
        message.setContent(body, "text/html; charset=UTF-8");

        Transport.send(message);
    }
}