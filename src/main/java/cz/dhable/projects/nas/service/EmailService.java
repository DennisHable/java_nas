package cz.dhable.projects.nas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // odesílatel (systémová schránka)

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * obecná metoda pro odeslání textového e-mailu
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
        } catch (Exception e) {
            // E-mail nesmí nikdy shodit zbytek aplikace, proto chybu pouze zalogujeme
            System.err.println("Critical error: Failed to send system email: " + e.getMessage());
        }
    }
}

