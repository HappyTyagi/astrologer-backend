package com.astro.backend.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.activation.DataSource;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String email, String otp) {
        String html = "<p>Your OTP is: <strong>" + otp + "</strong></p>";
        sendEmailAsync(email, "Your OTP Code", html);
    }

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            doSendEmail(toEmail, subject, htmlContent);
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
            doSendEmailWithAttachment(toEmail, subject, htmlContent, fileName, fileBytes, mimeType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendEmailAsync(String toEmail, String subject, String htmlContent) {
        try {
            doSendEmail(toEmail, subject, htmlContent);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Async email send failed. to={}, subject={}", toEmail, subject, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendEmailWithAttachmentAsync(
            String toEmail,
            String subject,
            String htmlContent,
            String fileName,
            byte[] fileBytes,
            String mimeType
    ) {
        try {
            doSendEmailWithAttachment(toEmail, subject, htmlContent, fileName, fileBytes, mimeType);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Async email attachment send failed. to={}, subject={}, file={}", toEmail, subject, fileName, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private void doSendEmail(String toEmail, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private void doSendEmailWithAttachment(
            String toEmail,
            String subject,
            String htmlContent,
            String fileName,
            byte[] fileBytes,
            String mimeType
    ) throws Exception {
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
    }
}
