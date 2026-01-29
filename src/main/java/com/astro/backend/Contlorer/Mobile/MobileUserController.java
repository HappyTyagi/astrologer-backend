package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.MobileUserRegistrationRequest;
import com.astro.backend.ResponseDTO.MobileUserProfileResponse;
import com.astro.backend.Services.MobileUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/user")
@RequiredArgsConstructor
public class MobileUserController {

    private final MobileUserService mobileUserService;

    /**
     * Register new mobile user
     * POST /api/mobile/user/register
     */
    @PostMapping("/register")
    public ResponseEntity<MobileUserProfileResponse> registerMobileUser(
            @Valid @RequestBody MobileUserRegistrationRequest request) {
        try {
            MobileUserProfileResponse response = mobileUserService.registerMobileUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MobileUserProfileResponse.builder()
                            .status(false)
                            .message("Registration failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get mobile user profile
     * GET /api/mobile/user/profile/{userId}
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<MobileUserProfileResponse> getMobileUserProfile(@PathVariable Long userId) {
        try {
            MobileUserProfileResponse response = mobileUserService.getMobileUserProfile(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(MobileUserProfileResponse.builder()
                            .status(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Update device details (token, fcmToken, appVersion)
     * PUT /api/mobile/user/device/{userId}
     */
    @PutMapping("/device/{userId}")
    public ResponseEntity<MobileUserProfileResponse> updateDeviceDetails(
            @PathVariable Long userId,
            @RequestParam String deviceToken,
            @RequestParam String fcmToken,
            @RequestParam String appVersion) {
        try {
            MobileUserProfileResponse response = mobileUserService.updateDeviceDetails(
                    userId, deviceToken, fcmToken, appVersion);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MobileUserProfileResponse.builder()
                            .status(false)
                            .message("Failed to update device details: " + e.getMessage())
                            .build());
        }
    }
}
