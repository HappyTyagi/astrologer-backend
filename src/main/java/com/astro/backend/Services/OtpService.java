package com.astro.backend.Services;

import com.astro.backend.Entity.OtpTransaction;
import com.astro.backend.Entity.User;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Repositry.OtpTransactionRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
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
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final SmsService smsService;
    private final Random random = new Random();

    /**
     * Generate and send OTP for mobile number
     * Returns sessionId (refNumber) for verification
     * Does NOT create user at this stage
     */
    @Transactional
    public OtpTransaction generateAndSendOtp(String mobileNumber) {

        // Generate random 6-digit OTP
        String otp = String.valueOf(100000 + random.nextInt(900000));

        // Generate unique session ID (UUID format like example)
        String sessionId = UUID.randomUUID().toString();

        // Create OTP Transaction record
        OtpTransaction otpTxn = OtpTransaction.builder()
                .mobileNumber(mobileNumber)
                .otp(otp)
                .refNumber(sessionId)  // Using refNumber as sessionId
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
     * Verify OTP - just marks as verified, doesn't create User
     * User will be created during profile update with all details
     * Returns true if OTP is valid, false otherwise
     */
    @Transactional
    public boolean verifyOtpOnly(String mobileNumber, String otp, String sessionId) {
        
        // Find OTP transaction by session ID (refNumber) and mobile number
        Optional<OtpTransaction> otpTxn = otpTransactionRepo
                .findByMobileNumberAndRefNumber(mobileNumber, sessionId);

        if (otpTxn.isEmpty()) {
            return false;
        }

        OtpTransaction txn = otpTxn.get();

        // Check if OTP is expired
        if (!txn.isValid()) {
            return false;
        }

        // Check if OTP matches
        if (!txn.getOtp().equals(otp)) {
            return false;
        }

        // Mark OTP as verified
        txn.setIsVerified(true);
        txn.setVerifiedAt(LocalDateTime.now());
        otpTransactionRepo.save(txn);
        
        return true;
    }

    /**
     * Verify OTP and return user details if valid
     * Creates user if doesn't exist
     */
    @Transactional
    public Optional<User> verifyOtpAndGetUser(String mobileNumber, String otp, String sessionId) {

        // Do NOT create User table entry here
        // OTP verification only marks OTP as valid
        // User will be created later when profile is completed
        
        // Return null/empty to indicate OTP was verified but no user created yet
        // Clients should call /profile/update after OTP verification
        return Optional.empty();
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

