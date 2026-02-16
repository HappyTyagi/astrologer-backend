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
        String html = buildEmailOtpTemplate(otp);
        sendEmailAsync(email, "Your Astrologer Email Verification OTP", html);
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

    private String buildEmailOtpTemplate(String otp) {
        String code = otp == null ? "------" : otp.trim();
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background:#f4f7fd;font-family:Calibri,Arial,sans-serif;color:#1f2431;">
                  <div style="max-width:680px;margin:0 auto;padding:18px 12px;">
                    <div style="background:#ffffff;border:1px solid #dfe7f5;border-radius:14px;overflow:hidden;">
                      <div style="padding:18px 20px;background:linear-gradient(90deg,#1f2f73,#3247a9);color:#ffffff;">
                        <h2 style="margin:0;font-size:22px;">Email Verification OTP</h2>
                        <p style="margin:8px 0 0 0;font-size:13px;color:#dce3ff;">
                          Use the OTP below to verify your email on Astrologer.
                        </p>
                      </div>
                      <div style="padding:18px 20px;">
                        <p style="margin:0 0 12px 0;font-size:14px;color:#4f5a72;">
                          Your One-Time Password:
                        </p>
                        <div style="display:inline-block;padding:10px 18px;border-radius:10px;background:#f2f6ff;border:1px dashed #9db2e5;">
                          <span style="letter-spacing:4px;font-size:28px;font-weight:700;color:#1f2f73;">%s</span>
                        </div>
                        <p style="margin:14px 0 0 0;font-size:13px;color:#4f5a72;">
                          This OTP is valid for <strong>10 minutes</strong>.
                        </p>
                        <p style="margin:10px 0 0 0;font-size:12px;color:#7b849a;">
                          For your security, do not share this OTP with anyone.
                        </p>
                      </div>
                      <div style="padding:12px 20px;background:#f8faff;border-top:1px solid #e5ecfb;">
                        <p style="margin:0;font-size:12px;color:#7b849a;">
                          Astrologer Security Team
                        </p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(code);
    }
}
