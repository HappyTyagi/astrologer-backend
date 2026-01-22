package com.astro.backend.Services;

import com.astro.backend.Entity.OtpTransaction;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.OtpTransactionRepository;
import com.astro.backend.Repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTransactionRepository otpTransactionRepo;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final Random random = new Random();

    /**
     * Generate and send OTP for mobile number
     * Check if user exists, if not create new user
     * Save OTP and reference number in OtpTransaction table
     * Send OTP via SMS
     */
    @Transactional
    public OtpTransaction generateAndSendOtp(String mobileNumber, String name) {

        // Check if user exists
        Optional<User> existingUser = userRepository.findByMobileNumber(mobileNumber);

        // If user doesn't exist, create new user
        if (existingUser.isEmpty()) {
            User newUser = User.builder()
                    .name(name)
                    .mobileNumber(mobileNumber)
                    .isVerified(false)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(newUser);
        }

        // Generate random 6-digit OTP
        String otp = String.valueOf(100000 + random.nextInt(900000));

        // Generate unique reference number
        String refNumber = UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Create OTP Transaction record
        OtpTransaction otpTxn = OtpTransaction.builder()
                .mobileNumber(mobileNumber)
                .otp(otp)
                .refNumber(refNumber)
                .isVerified(false)
                .build();

        otpTxn = otpTransactionRepo.save(otpTxn);

        // Send OTP via SMS
        try {
            smsService.sendOtpSms(mobileNumber, otp);
        } catch (Exception e) {
            // Log but don't fail - OTP is still saved
            System.err.println("Failed to send SMS: " + e.getMessage());
        }

        return otpTxn;
    }

    /**
     * Verify OTP and return user details if valid
     */
    @Transactional
    public Optional<User> verifyOtp(String mobileNumber, String otp, String refNumber) {

        // Find OTP transaction by reference number and mobile number
        Optional<OtpTransaction> otpTxn = otpTransactionRepo
                .findByMobileNumberAndRefNumber(mobileNumber, refNumber);

        if (otpTxn.isEmpty()) {
            return Optional.empty();
        }

        OtpTransaction txn = otpTxn.get();

        // Check if OTP is expired
        if (!txn.isValid()) {
            return Optional.empty();
        }

        // Check if OTP matches
        if (!txn.getOtp().equals(otp)) {
            return Optional.empty();
        }

        // Mark OTP as verified
        txn.setIsVerified(true);
        txn.setVerifiedAt(LocalDateTime.now());
        otpTransactionRepo.save(txn);

        // Get user by mobile number
        Optional<User> user = userRepository.findByMobileNumber(mobileNumber);

        if (user.isPresent()) {
            User u = user.get();
            // Mark user as verified
            u.setIsVerified(true);
            u.setUpdatedAt(LocalDateTime.now());
            userRepository.save(u);
        }

        return user;
    }

    /**
     * Check if OTP transaction exists and is valid
     */
    public boolean isOtpValid(String mobileNumber, String otp, String refNumber) {

        Optional<OtpTransaction> otpTxn = otpTransactionRepo
                .findByMobileNumberAndRefNumber(mobileNumber, refNumber);

        if (otpTxn.isEmpty()) {
            return false;
        }

        OtpTransaction txn = otpTxn.get();

        // Check if expired and not already verified
        if (!txn.isValid()) {
            return false;
        }

        // Check if OTP matches
        return txn.getOtp().equals(otp);
    }

    /**
     * Clean up expired OTPs
     */
    @Transactional
    public void cleanupExpiredOtps() {
        otpTransactionRepo.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}

