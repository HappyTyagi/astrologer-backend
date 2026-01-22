package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Auth.JwtService;
import com.astro.backend.Entity.OtpTransaction;
import com.astro.backend.Entity.User;
import com.astro.backend.Helper.AstrologyHelper;
import com.astro.backend.RequestDTO.SendOtpRequest;
import com.astro.backend.RequestDTO.VerifyOtpRequest;
import com.astro.backend.ResponseDTO.SendOtpResponse;
import com.astro.backend.ResponseDTO.VerifyOtpResponse;
import com.astro.backend.Services.OtpService;
import com.astro.backend.Repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Send OTP to mobile number
     * - Check if user exists with mobile number
     * - If not exist: insert new user
     * - If exist: don't insert
     * - Generate random 6-digit OTP
     * - Generate reference number
     * - Save in OtpTransaction table
     * - Send OTP via SMS
     */
    @PostMapping("/send")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {

        try {
            String mobileNumber = AstrologyHelper.sanitizeString(request.getMobileNumber());
            String name = AstrologyHelper.sanitizeString(request.getName());

            // Check if user exists
            Optional<User> existingUser = userRepository.findByMobileNumber(mobileNumber);
            boolean isNewUser = existingUser.isEmpty();

            // Generate OTP and save in transaction table
            OtpTransaction otpTxn = otpService.generateAndSendOtp(mobileNumber, name);

            SendOtpResponse response = SendOtpResponse.builder()
                    .refNumber(otpTxn.getRefNumber())
                    .message("OTP sent successfully to " + AstrologyHelper.maskMobileNumber(mobileNumber))
                    .isNewUser(isNewUser)
                    .mobileNumber(AstrologyHelper.maskMobileNumber(mobileNumber))
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SendOtpResponse.builder()
                            .message("Failed to send OTP: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Verify OTP
     * - Get OTP, reference number, and mobile number
     * - Check if OTP is valid (not expired, matches stored value)
     * - If valid: create JWT token with mobile number and name
     * - Return JWT token and user details
     */
    @PostMapping("/verify")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {

        try {
            String mobileNumber = AstrologyHelper.sanitizeString(request.getMobileNumber());
            String otp = AstrologyHelper.sanitizeString(request.getOtp());
            String refNumber = AstrologyHelper.sanitizeString(request.getRefNumber());

            // Verify OTP
            Optional<User> user = otpService.verifyOtp(mobileNumber, otp, refNumber);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(VerifyOtpResponse.builder()
                                .isValid(false)
                                .message("Invalid OTP or reference number")
                                .build());
            }

            User verifiedUser = user.get();

            // Generate JWT token with mobile number and name
            // Token expires in 7 days (7 * 24 * 60 * 60 * 1000 ms)
            long expiryMs = 7 * 24 * 60 * 60 * 1000L;
            String token = jwtService.generateTokenWithClaims(
                    verifiedUser.getMobileNumber(),
                    verifiedUser.getName(),
                    expiryMs
            );

            // Generate refresh token (30 days)
            long refreshExpiryMs = 30 * 24 * 60 * 60 * 1000L;
            String refreshToken = jwtService.generateTokenWithClaims(
                    verifiedUser.getMobileNumber(),
                    verifiedUser.getName(),
                    refreshExpiryMs
            );

            VerifyOtpResponse response = VerifyOtpResponse.builder()
                    .isValid(true)
                    .message("OTP verified successfully")
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .userId(verifiedUser.getId())
                    .mobileNumber(verifiedUser.getMobileNumber())
                    .name(verifiedUser.getName())
                    .email(verifiedUser.getEmail())
                    .role(verifiedUser.getRole().toString())
                    .isNewUser(false)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VerifyOtpResponse.builder()
                            .isValid(false)
                            .message("Failed to verify OTP: " + e.getMessage())
                            .build());
        }
    }
}
