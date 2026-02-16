package com.astro.backend.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String email, String otp) {
        String html = "<p>Your OTP is: <strong>" + otp + "</strong></p>";
        sendEmail(email, "Your OTP Code", html);
    }

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendEmailWithAttachment(
            String toEmail,
            String subject,
            String htmlContent,
            String fileName,
            byte[] fileBytes,
            String mimeType
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (fileBytes != null && fileBytes.length > 0) {
                String safeFileName = fileName == null || fileName.isBlank() ? "attachment.pdf" : fileName;
                String safeMime = mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType;
                DataSource dataSource = new ByteArrayDataSource(fileBytes, safeMime);
                helper.addAttachment(safeFileName, dataSource);
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }
}
