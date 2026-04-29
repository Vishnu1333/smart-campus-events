package com.smartcampus.eventmanagement.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${email.from-address:noreply@smartcampus.com}")
    private String fromAddress;

    private boolean isMockMode = false;

    @PostConstruct
    public void init() {
        if (isBlank(mailHost)) {
            System.out.println("EmailService: Running in MOCK mode. Emails will only be printed to console.");
            isMockMode = true;
        }
    }

    public boolean sendEmail(String toEmail, String subject, String text) {
        if (isBlank(toEmail) || isBlank(text)) {
            System.err.println("Email not sent: recipient and message are required.");
            return false;
        }

        if (isMockMode || mailSender == null) {
            System.out.println("==================================================");
            System.out.println("MOCK EMAIL SENT TO: " + toEmail);
            System.out.println("SUBJECT: " + subject);
            System.out.println("MESSAGE: \n" + text);
            System.out.println("==================================================");
            return true;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send Email: " + e.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
