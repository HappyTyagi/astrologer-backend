package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.GenderMasterRequest;
import com.astro.backend.ResponseDTO.GenderMasterResponse;
import com.astro.backend.Services.GenderMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/master/gender")
@RequiredArgsConstructor
public class GenderMasterController {

    private final GenderMasterService genderService;

    /**
     * Get all genders
     */
    @GetMapping("/all")
    public ResponseEntity<List<GenderMasterResponse>> getAllGenders() {
        try {
            List<GenderMasterResponse> genders = genderService.getAllGenders();
            return ResponseEntity.ok(genders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get active genders only
     */
    @GetMapping("/active")
    public ResponseEntity<List<GenderMasterResponse>> getActiveGenders() {
        try {
            List<GenderMasterResponse> genders = genderService.getActiveGenders();
            return ResponseEntity.ok(genders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get gender by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GenderMasterResponse> getGenderById(@PathVariable Long id) {
        try {
            GenderMasterResponse gender = genderService.getGenderById(id);
            return ResponseEntity.ok(gender);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Create new gender
     */
    @PostMapping("/create")
    public ResponseEntity<GenderMasterResponse> createGender(@Valid @RequestBody GenderMasterRequest request) {
        try {
            GenderMasterResponse response = genderService.createGender(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to create gender: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update gender
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<GenderMasterResponse> updateGender(@PathVariable Long id, 
            @Valid @RequestBody GenderMasterRequest request) {
        try {
            GenderMasterResponse response = genderService.updateGender(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to update gender: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Delete gender
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GenderMasterResponse> deleteGender(@PathVariable Long id) {
        try {
            GenderMasterResponse response = genderService.deleteGender(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to delete gender: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Toggle active status
     */
    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<GenderMasterResponse> toggleStatus(@PathVariable Long id, 
            @RequestParam Boolean isActive) {
        try {
            GenderMasterResponse response = genderService.toggleActiveStatus(id, isActive);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to toggle status: " + e.getMessage())
                            .build());
        }
    }
}
