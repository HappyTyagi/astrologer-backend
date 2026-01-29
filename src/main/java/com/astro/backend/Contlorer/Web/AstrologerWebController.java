package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.AstrologerApprovalRequest;
import com.astro.backend.RequestDTO.AstrologerProfileRequest;
import com.astro.backend.RequestDTO.AstrologerSearchRequest;
import com.astro.backend.ResponseDTO.AstrologerProfileResponse;
import com.astro.backend.Services.AstrologerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Web/CMS Controller for Astrologer Profile Management
 * Admin endpoints for managing astrologer profiles
 */
@RestController
@RequestMapping("/api/web/astrologers")
@RequiredArgsConstructor
public class AstrologerWebController {

    private final AstrologerProfileService astrologerProfileService;

    /**
     * Get all astrologers (including inactive)
     */
    @GetMapping("/all")
    public ResponseEntity<List<AstrologerProfileResponse>> getAllAstrologers() {
        try {
            List<AstrologerProfileResponse> astrologers = astrologerProfileService.getAllActiveAstrologers();
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get astrologer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AstrologerProfileResponse> getAstrologerById(@PathVariable Long id) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.getProfileById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get astrologer by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<AstrologerProfileResponse> getAstrologerByUserId(@PathVariable Long userId) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.getProfileByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Create astrologer profile
     */
    @PostMapping("/create")
    public ResponseEntity<AstrologerProfileResponse> createAstrologer(
            @Valid @RequestBody AstrologerProfileRequest request) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.createProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to create profile: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update astrologer profile
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<AstrologerProfileResponse> updateAstrologer(
            @PathVariable Long id,
            @Valid @RequestBody AstrologerProfileRequest request) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.updateProfile(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to update profile: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Approve or reject astrologer
     */
    @PutMapping("/approve/{id}")
    public ResponseEntity<AstrologerProfileResponse> approveAstrologer(
            @PathVariable Long id,
            @Valid @RequestBody AstrologerApprovalRequest request) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.approveAstrologer(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to approve astrologer: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Verify astrologer (email/phone/identity verification)
     */
    @PutMapping("/verify/{id}")
    public ResponseEntity<AstrologerProfileResponse> verifyAstrologer(
            @PathVariable Long id,
            @RequestParam Boolean isVerified) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.verifyAstrologer(id, isVerified);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to verify astrologer: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get pending approval astrologers
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AstrologerProfileResponse>> getPendingApprovals() {
        try {
            List<AstrologerProfileResponse> astrologers = astrologerProfileService.getPendingApprovals();
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Toggle astrologer active status
     */
    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<AstrologerProfileResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        try {
            AstrologerProfileResponse current = astrologerProfileService.getProfileById(id);
            
            AstrologerProfileRequest request = AstrologerProfileRequest.builder()
                    .userId(current.getUserId())
                    .consultationRate(current.getConsultationRate())
                    .bio(current.getBio())
                    .certifications(current.getCertifications())
                    .languages(current.getLanguages())
                    .specializations(current.getSpecializations())
                    .experienceYears(current.getExperienceYears())
                    .maxConcurrentChats(current.getMaxConcurrentChats())
                    .isAvailable(current.getIsAvailable())
                    .isActive(isActive)
                    .build();
            
            AstrologerProfileResponse response = astrologerProfileService.updateProfile(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to toggle status: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Search astrologers (admin view - includes all statuses)
     */
    @PostMapping("/search")
    public ResponseEntity<List<AstrologerProfileResponse>> searchAstrologers(
            @RequestBody AstrologerSearchRequest searchRequest) {
        try {
            List<AstrologerProfileResponse> astrologers = astrologerProfileService.searchAstrologers(searchRequest);
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Delete astrologer profile (soft delete)
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<AstrologerProfileResponse> deleteAstrologer(@PathVariable Long id) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.deleteProfile(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to delete profile: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get available astrologers
     */
    @GetMapping("/available")
    public ResponseEntity<List<AstrologerProfileResponse>> getAvailableAstrologers() {
        try {
            List<AstrologerProfileResponse> astrologers = astrologerProfileService.getAvailableAstrologers();
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Toggle astrologer availability
     */
    @PutMapping("/availability/{id}")
    public ResponseEntity<AstrologerProfileResponse> toggleAvailability(
            @PathVariable Long id,
            @RequestParam Boolean isAvailable) {
        try {
            AstrologerProfileResponse response = astrologerProfileService.toggleAvailability(id, isAvailable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to update availability: " + e.getMessage())
                            .build());
        }
    }
}
