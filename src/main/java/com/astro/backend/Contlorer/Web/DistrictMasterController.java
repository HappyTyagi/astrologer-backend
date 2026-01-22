package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.DistrictMaster;
import com.astro.backend.Repositry.DistrictMasterRepository;
import com.astro.backend.RequestDTO.DistrictMasterRequest;
import com.astro.backend.ResponseDTO.DistrictMasterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/master/district")
@RequiredArgsConstructor
public class DistrictMasterController {

    private final DistrictMasterRepository districtRepository;

    /**
     * Get all districts
     */
    @GetMapping("/all")
    public ResponseEntity<List<DistrictMaster>> getAllDistricts() {
        try {
            List<DistrictMaster> districts = districtRepository.findAll();
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active districts only
     */
    @GetMapping("/active")
    public ResponseEntity<List<DistrictMaster>> getActiveDistricts() {
        try {
            List<DistrictMaster> districts = districtRepository.findByIsActiveOrderByName(true);
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get districts by state ID
     */
    @GetMapping("/state/{stateId}")
    public ResponseEntity<List<DistrictMaster>> getDistrictsByState(@PathVariable Long stateId) {
        try {
            List<DistrictMaster> districts = districtRepository.findByStateIdOrderByName(stateId);
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get district by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DistrictMaster> getDistrictById(@PathVariable Long id) {
        try {
            DistrictMaster district = districtRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("District not found"));
            return ResponseEntity.ok(district);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Insert new district
     */
    @PostMapping("/insert")
    public ResponseEntity<DistrictMasterResponse> insertDistrict(@Valid @RequestBody DistrictMasterRequest request) {
        try {
            DistrictMaster district = DistrictMaster.builder()
                    .stateId(request.getStateId())
                    .name(request.getName())
                    .code(request.getCode())
                    .description(request.getDescription())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            DistrictMaster savedDistrict = districtRepository.save(district);

            return ResponseEntity.status(HttpStatus.CREATED).body(DistrictMasterResponse.builder()
                    .id(savedDistrict.getId())
                    .stateId(savedDistrict.getStateId())
                    .name(savedDistrict.getName())
                    .code(savedDistrict.getCode())
                    .description(savedDistrict.getDescription())
                    .isActive(savedDistrict.getIsActive())
                    .createdAt(savedDistrict.getCreatedAt())
                    .status(true)
                    .message("District inserted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DistrictMasterResponse.builder()
                            .status(false)
                            .message("Failed to insert district: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update district
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<DistrictMasterResponse> updateDistrict(@PathVariable Long id, 
            @Valid @RequestBody DistrictMasterRequest request) {
        try {
            DistrictMaster district = districtRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("District not found"));

            district.setStateId(request.getStateId());
            district.setName(request.getName());
            district.setCode(request.getCode());
            district.setDescription(request.getDescription());
            if (request.getIsActive() != null) {
                district.setIsActive(request.getIsActive());
            }

            DistrictMaster updatedDistrict = districtRepository.save(district);

            return ResponseEntity.ok(DistrictMasterResponse.builder()
                    .id(updatedDistrict.getId())
                    .stateId(updatedDistrict.getStateId())
                    .name(updatedDistrict.getName())
                    .code(updatedDistrict.getCode())
                    .description(updatedDistrict.getDescription())
                    .isActive(updatedDistrict.getIsActive())
                    .updatedAt(updatedDistrict.getUpdatedAt())
                    .status(true)
                    .message("District updated successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DistrictMasterResponse.builder()
                            .status(false)
                            .message("Failed to update district: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Delete district
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<DistrictMasterResponse> deleteDistrict(@PathVariable Long id) {
        try {
            DistrictMaster district = districtRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("District not found"));

            districtRepository.deleteById(id);

            return ResponseEntity.ok(DistrictMasterResponse.builder()
                    .id(district.getId())
                    .name(district.getName())
                    .status(true)
                    .message("District deleted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DistrictMasterResponse.builder()
                            .status(false)
                            .message("Failed to delete district: " + e.getMessage())
                            .build());
        }
    }
}
