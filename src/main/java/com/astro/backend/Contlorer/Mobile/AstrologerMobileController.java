package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.AstrologerSearchRequest;
import com.astro.backend.ResponseDTO.AstrologerProfileResponse;
import com.astro.backend.Services.AstrologerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mobile API for Astrologer Profiles
 * Public-facing endpoints for users to discover and view astrologers
 */
@RestController
@RequestMapping("/api/mobile/astrologers")
@RequiredArgsConstructor
public class AstrologerMobileController {

    private final AstrologerProfileService astrologerProfileService;

    /**
     * Get all active astrologers
     */
    @GetMapping
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
     * Get astrologer details by ID
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
     * Get available astrologers (online/ready to consult)
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
     * Search astrologers with filters
     * Query params: specialization, language, minRating, maxRate, availableOnly, sortBy
     */
    @GetMapping("/search")
    public ResponseEntity<List<AstrologerProfileResponse>> searchAstrologers(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRate,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) String sortBy) {
        try {
            AstrologerSearchRequest searchRequest = AstrologerSearchRequest.builder()
                    .specialization(specialization)
                    .language(language)
                    .minRating(minRating)
                    .maxRate(maxRate)
                    .availableOnly(availableOnly)
                    .sortBy(sortBy)
                    .build();

            List<AstrologerProfileResponse> astrologers = astrologerProfileService.searchAstrologers(searchRequest);
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get astrologers by specialization
     */
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<AstrologerProfileResponse>> getBySpecialization(
            @PathVariable String specialization) {
        try {
            AstrologerSearchRequest searchRequest = AstrologerSearchRequest.builder()
                    .specialization(specialization)
                    .build();

            List<AstrologerProfileResponse> astrologers = astrologerProfileService.searchAstrologers(searchRequest);
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get top rated astrologers
     */
    @GetMapping("/top-rated")
    public ResponseEntity<List<AstrologerProfileResponse>> getTopRatedAstrologers(
            @RequestParam(defaultValue = "4.0") Double minRating) {
        try {
            AstrologerSearchRequest searchRequest = AstrologerSearchRequest.builder()
                    .minRating(minRating)
                    .sortBy("rating")
                    .build();

            List<AstrologerProfileResponse> astrologers = astrologerProfileService.searchAstrologers(searchRequest);
            return ResponseEntity.ok(astrologers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Toggle astrologer availability (for astrologer's own profile)
     * @param userId - The user ID of the astrologer
     * @param isAvailable - Availability status
     */
    @PutMapping("/availability/{userId}")
    public ResponseEntity<AstrologerProfileResponse> toggleAvailability(
            @PathVariable Long userId,
            @RequestParam Boolean isAvailable) {
        try {
            AstrologerProfileResponse profileResponse = astrologerProfileService.getProfileByUserId(userId);
            AstrologerProfileResponse response = astrologerProfileService.toggleAvailability(
                    profileResponse.getId(), isAvailable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AstrologerProfileResponse.builder()
                            .status(false)
                            .message("Failed to update availability: " + e.getMessage())
                            .build());
        }
    }
}
