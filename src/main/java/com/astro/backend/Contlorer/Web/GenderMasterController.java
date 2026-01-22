package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.GenderMaster;
import com.astro.backend.Repositry.GenderMasterRepository;
import com.astro.backend.RequestDTO.GenderMasterRequest;
import com.astro.backend.ResponseDTO.GenderMasterResponse;
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

    private final GenderMasterRepository genderRepository;

    /**
     * Get all genders
     */
    @GetMapping("/all")
    public ResponseEntity<List<GenderMaster>> getAllGenders() {
        try {
            List<GenderMaster> genders = genderRepository.findAll();
            return ResponseEntity.ok(genders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active genders only
     */
    @GetMapping("/active")
    public ResponseEntity<List<GenderMaster>> getActiveGenders() {
        try {
            List<GenderMaster> genders = genderRepository.findByIsActiveOrderByName(true);
            return ResponseEntity.ok(genders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get gender by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GenderMaster> getGenderById(@PathVariable Long id) {
        try {
            GenderMaster gender = genderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Gender not found"));
            return ResponseEntity.ok(gender);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Insert new gender
     */
    @PostMapping("/insert")
    public ResponseEntity<GenderMasterResponse> insertGender(@Valid @RequestBody GenderMasterRequest request) {
        try {
            GenderMaster gender = GenderMaster.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            GenderMaster savedGender = genderRepository.save(gender);

            return ResponseEntity.status(HttpStatus.CREATED).body(GenderMasterResponse.builder()
                    .id(savedGender.getId())
                    .name(savedGender.getName())
                    .description(savedGender.getDescription())
                    .isActive(savedGender.getIsActive())
                    .createdAt(savedGender.getCreatedAt())
                    .status(true)
                    .message("Gender inserted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to insert gender: " + e.getMessage())
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
            GenderMaster gender = genderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Gender not found"));

            gender.setName(request.getName());
            gender.setDescription(request.getDescription());
            if (request.getIsActive() != null) {
                gender.setIsActive(request.getIsActive());
            }

            GenderMaster updatedGender = genderRepository.save(gender);

            return ResponseEntity.ok(GenderMasterResponse.builder()
                    .id(updatedGender.getId())
                    .name(updatedGender.getName())
                    .description(updatedGender.getDescription())
                    .isActive(updatedGender.getIsActive())
                    .updatedAt(updatedGender.getUpdatedAt())
                    .status(true)
                    .message("Gender updated successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
            GenderMaster gender = genderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Gender not found"));

            genderRepository.deleteById(id);

            return ResponseEntity.ok(GenderMasterResponse.builder()
                    .id(gender.getId())
                    .name(gender.getName())
                    .status(true)
                    .message("Gender deleted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenderMasterResponse.builder()
                            .status(false)
                            .message("Failed to delete gender: " + e.getMessage())
                            .build());
        }
    }
}
