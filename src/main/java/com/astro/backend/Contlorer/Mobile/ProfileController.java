package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.CompleteProfileRequest;
import com.astro.backend.RequestDTO.UpdateProfileRequest;
import com.astro.backend.ResponseDTO.UpdateProfileResponse;
import com.astro.backend.Services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "User profile operations")
public class ProfileController {

    private final ProfileService profileService;
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    /**
     * Complete user profile after OTP verification
     * For users with isProfileComplete=false
     * Accepts: mobile number, name, DOB, birth time, gender, location details
     */
    @PostMapping("/complete")
    @Operation(summary = "Complete user profile", 
               description = "Complete profile for new users after OTP verification with all required details")
    public ResponseEntity<UpdateProfileResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        try {
            // Print received request
            logger.info("\n========== /profile/complete REQUEST ==========");
            logger.info("Mobile No: {}", request.getMobileNo());
            logger.info("Name: {}", request.getName());
            logger.info("DOB: {}", request.getDob());
            logger.info("Birth Time: {} {}", request.getBirthTime(), request.getAmPm());
            logger.info("Gender: {}", request.getGender());
            logger.info("State ID: {}", request.getStateId());
            logger.info("District ID: {}", request.getDistrictId());
            logger.info("Latitude: {}", request.getLatitude());
            logger.info("Longitude: {}", request.getLongitude());
            logger.info("Address: {}", request.getAddress());
            logger.info("============================================\n");
            
            UpdateProfileResponse response = profileService.completeProfile(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(UpdateProfileResponse.builder()
                            .status(false)
                            .message("Failed to complete profile: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update user profile after registration
     * Accepts: userId, dateOfBirth, gender, city, state, latitude (optional), longitude (optional)
     * Calculates and saves age to database
     */
    @PostMapping("/update")
    @Operation(summary = "Update user profile", 
               description = "Update existing user profile with additional details")
    public ResponseEntity<UpdateProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            // Print received request
            logger.info("\n========== /profile/update REQUEST ==========");
            logger.info("User ID: {}", request.getUserId());
            logger.info("Name: {}", request.getName());
            logger.info("Mobile No: {}", request.getMobileNo());
            logger.info("Device Token: {}", request.getDeviceToken());
            logger.info("FCM Token: {}", request.getFcmToken());
            logger.info("Device ID: {}", request.getDeviceId());
            logger.info("App Version: {}", request.getAppVersion());
            logger.info("OS Type: {}", request.getOsType());
            logger.info("DOB: {}", request.getDateOfBirth());
            String amPm = request.getBirthAmPm() != null ? request.getBirthAmPm() : request.getAmPm();
            logger.info("Birth Time: {} {}", request.getBirthTime(), amPm);
            logger.info("Age: {}", request.getAge());
            logger.info("Gender Master ID: {}", request.getGenderMasterId());
            logger.info("State Master ID: {}", request.getStateMasterId());
            logger.info("District Master ID: {}", request.getDistrictMasterId());
            logger.info("Latitude: {}", request.getLatitude());
            logger.info("Longitude: {}", request.getLongitude());
            logger.info("Address: {}", request.getAddress());
            logger.info("=========================================\n");
            
            UpdateProfileResponse response = profileService.updateProfile(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UpdateProfileResponse.builder()
                            .status(false)
                            .message("Failed to update profile: " + e.getMessage())
                            .build());
        }
    }
}
