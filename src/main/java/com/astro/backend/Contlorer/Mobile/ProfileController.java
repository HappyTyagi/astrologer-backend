package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.UpdateProfileRequest;
import com.astro.backend.ResponseDTO.UpdateProfileResponse;
import com.astro.backend.Services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Update user profile after registration
     * Accepts: userId, dateOfBirth, gender, city, state, latitude (optional), longitude (optional)
     * Calculates and saves age to database
     */
    @PutMapping("/update")
    public ResponseEntity<UpdateProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
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
