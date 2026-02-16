package com.astro.backend.Services;

import com.astro.backend.Entity.EmailOtpTransaction;
import com.astro.backend.Repositry.EmailOtpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final EmailOtpTransactionRepository emailOtpTransactionRepository;
    private final EmailService emailService;

    public EmailOtpTransaction generateAndSendOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        EmailOtpTransaction txn = EmailOtpTransaction.builder()
                .email(email)
                .otp(otp)
                .refNumber(sessionId)
                .isVerified(false)
                .createdAt(now)
                .expiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        EmailOtpTransaction saved = emailOtpTransactionRepository.save(txn);
        emailService.sendOtpEmail(email, otp);
        return saved;
    }

    public boolean verifyOtp(String rawEmail, String otp, String sessionId) {
        String email = normalizeEmail(rawEmail);
        String safeOtp = otp == null ? "" : otp.trim();
        String safeSession = sessionId == null ? "" : sessionId.trim();
        if (email.isEmpty() || safeOtp.isEmpty() || safeSession.isEmpty()) {
            return false;
        }

        EmailOtpTransaction txn = emailOtpTransactionRepository
                .findFirstByRefNumberAndEmailOrderByIdDesc(safeSession, email)
                .orElse(null);
        if (txn == null || Boolean.TRUE.equals(txn.getIsVerified())) {
            return false;
        }
        if (txn.isExpired()) {
            return false;
        }
        if (!safeOtp.equals(txn.getOtp())) {
            return false;
        }

        txn.setIsVerified(true);
        txn.setVerifiedAt(LocalDateTime.now());
        emailOtpTransactionRepository.save(txn);
        return true;
    }

    public void assertVerifiedAndConsume(String rawEmail, String rawSessionId) {
        String email = normalizeEmail(rawEmail);
        String sessionId = rawSessionId == null ? "" : rawSessionId.trim();
        if (email.isEmpty() || sessionId.isEmpty()) {
            throw new RuntimeException("Email OTP verification is required.");
        }

        EmailOtpTransaction txn = emailOtpTransactionRepository
                .findFirstByRefNumberAndEmailOrderByIdDesc(sessionId, email)
                .orElseThrow(() -> new RuntimeException("Email OTP session not found. Please verify email again."));

        if (!Boolean.TRUE.equals(txn.getIsVerified())) {
            throw new RuntimeException("Email OTP is not verified.");
        }
        if (txn.isExpired()) {
            throw new RuntimeException("Email OTP session expired. Please verify email again.");
        }
        if (txn.getConsumedAt() != null) {
            throw new RuntimeException("Email OTP session already used. Please verify email again.");
        }

        txn.setConsumedAt(LocalDateTime.now());
        emailOtpTransactionRepository.save(txn);
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
    }
}

