package business.service.mail;

public interface MailSender {
    void sendActivationCode(String toEmail, String employeeName, String activationCode) throws Exception;
}