package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.OtpTransaction;
import com.astro.backend.Helper.AstrologyHelper;
import com.astro.backend.RequestDTO.SendOtpRequest;
import com.astro.backend.RequestDTO.VerifyOtpRequest;
import com.astro.backend.ResponseDTO.SendOtpResponse;
import com.astro.backend.ResponseDTO.VerifyOtpResponse;
import com.astro.backend.Services.OtpService;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP Authentication", description = "Mobile OTP-based authentication endpoints")
public class OtpController {

    private final OtpService otpService;
    private final MobileUserProfileRepository mobileUserProfileRepository;

    /**
     * Send OTP to mobile number (mLogin)
     * POST /otp/send
     * Request: { "mobileNo": "7906396608" }
     * Response: { "sessionId": "uuid", "message": "OTP sent", "mobileNo": "79****6608", "success": true }
     */
    @PostMapping("/send")
    @Operation(summary = "Send OTP", description = "Send OTP to mobile number for authentication")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {

        try {
            if (request == null || request.getMobileNo() == null || request.getMobileNo().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(SendOtpResponse.builder()
                                .message("Mobile number is required")
                                .success(false)
                                .build());
            }

            String mobileNumber = AstrologyHelper.sanitizeString(request.getMobileNo());

            // Validate mobile number format (10 digits)
            if (!mobileNumber.matches("^[0-9]{10}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(SendOtpResponse.builder()
                                .message("Invalid mobile number format. Must be 10 digits")
                                .mobileNo(mobileNumber)
                                .success(false)
                                .build());
            }

            // Generate OTP and save in transaction table
            OtpTransaction otpTxn = otpService.generateAndSendOtp(mobileNumber);

            if (otpTxn == null || otpTxn.getRefNumber() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(SendOtpResponse.builder()
                                .message("Failed to generate OTP")
                                .success(false)
                                .build());
            }

            SendOtpResponse response = SendOtpResponse.builder()
                    .sessionId(otpTxn.getRefNumber())
                    .message("OTP sent successfully")
                    .mobileNo(mobileNumber)
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SendOtpResponse.builder()
                            .message("Failed to send OTP: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    /**
     * Verify OTP and return JWT token
     * POST /otp/verify
     * Request: { "otp": "521649", "sessionId": "uuid", "mobileNo": "7906396608" }
     * Response: { "success": true, "token": "jwt", "refreshToken": "jwt", "userId": 1, "name": "User", "mobileNo": "7906396608", "isNewUser": false }
     * 
     * If user doesn't exist, creates new user with mobile number
     * Returns JWT token for authentication
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP. User will be created after profile update.")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {

        try {
            String mobileNumber = AstrologyHelper.sanitizeString(request.getMobileNo());
            String otp = AstrologyHelper.sanitizeString(request.getOtp());
            String sessionId = AstrologyHelper.sanitizeString(request.getSessionId());

            // Verify OTP only (does not create user)
            boolean isOtpValid = otpService.verifyOtpOnly(mobileNumber, otp, sessionId);

            if (!isOtpValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(VerifyOtpResponse.builder()
                                .success(false)
                                .message("Invalid OTP or session")
                                .build());
            }

                    // OTP verified successfully
                    // Check if mobile profile already exists by mobile number (no User table check)
                    Optional<MobileUserProfile> profileOpt = mobileUserProfileRepository.findByMobileNumber(mobileNumber);
                    if (profileOpt.isPresent()) {
                    MobileUserProfile profile = profileOpt.get();
                    boolean profileComplete = Boolean.TRUE.equals(profile.getIsProfileComplete()) || hasAllRequiredFields(profile);

                    VerifyOtpResponse response = VerifyOtpResponse.builder()
                        .success(true)
                        .message(profileComplete
                            ? "OTP verified successfully. Profile is complete."
                            : "OTP verified successfully. Proceed to complete profile.")
                        .userId(profile.getUserId())
                        .name(profile.getName())
                        .email(profile.getEmail())
                        .mobileNo(mobileNumber)
                        .isNewUser(false)
                        .isProfileComplete(profileComplete)
                        // Include profile data for SharedPreferences
                        .dateOfBirth(profile.getDateOfBirth())
                        .age(profile.getAge())
                        .genderMasterId(profile.getGenderMasterId())
                        .stateMasterId(profile.getStateMasterId())
                        .districtMasterId(profile.getDistrictMasterId())
                        .latitude(profile.getLatitude())
                        .longitude(profile.getLongitude())
                        .birthTime(profile.getBirthTime())
                        .birthAmPm(profile.getBirthAmPm())
                        .address(profile.getAddress())
                        .isLanguage(profile.getIsLanguage())
                        .build();

                    return ResponseEntity.ok(response);
                    }

                    VerifyOtpResponse response = VerifyOtpResponse.builder()
                        .success(true)
                        .message("OTP verified successfully. Proceed to complete profile.")
                        .mobileNo(mobileNumber)
                        .isNewUser(true)
                        .isProfileComplete(false)
                        .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VerifyOtpResponse.builder()
                            .success(false)
                            .message("Failed to verify OTP: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Check if mobile user profile has all required fields
     * Required fields: name (in User), genderMasterId, stateMasterId, districtMasterId
     */
    private boolean hasAllRequiredFields(MobileUserProfile profile) {
        return profile != null &&
                profile.getName() != null && !profile.getName().isEmpty() &&
                profile.getGenderMasterId() != null &&
                profile.getStateMasterId() != null &&
                profile.getDistrictMasterId() != null;
    }
}
