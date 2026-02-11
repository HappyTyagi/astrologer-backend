package com.astro.backend.Services;

import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.SendEmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    public void sendOtpEmail(String email, String otp) {
        String html = "<p>Your OTP is: <strong>" + otp + "</strong></p>";
        sendEmail(email, "Your OTP Code", html);
    }

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Resend resend = new Resend(apiKey);
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .from(fromEmail)
                    .to(List.of(toEmail))
                    .subject(subject)
                    .html(htmlContent)
                    .build();
            resend.emails().send(sendEmailRequest);
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
            Resend resend = new Resend(apiKey);
            SendEmailRequest.Builder builder = SendEmailRequest.builder()
                    .from(fromEmail)
                    .to(List.of(toEmail))
                    .subject(subject)
                    .html(htmlContent);

            if (fileBytes != null && fileBytes.length > 0) {
                Attachment attachment = Attachment.builder()
                        .fileName(fileName == null || fileName.isBlank() ? "attachment.pdf" : fileName)
                        .content(Base64.getEncoder().encodeToString(fileBytes))
                        .build();
                builder.attachments(List.of(attachment));
            }

            resend.emails().send(builder.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }
}
