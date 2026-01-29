package com.astro.backend.Services;

import com.astro.backend.Entity.GenderMaster;
import com.astro.backend.Repositry.GenderMasterRepository;
import com.astro.backend.RequestDTO.GenderMasterRequest;
import com.astro.backend.ResponseDTO.GenderMasterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Gender Master CRUD operations
 * All business logic is handled here
 */
@Service
@RequiredArgsConstructor
public class GenderMasterService {

    private final GenderMasterRepository genderRepository;

    /**
     * Get all genders from database
     */
    public List<GenderMasterResponse> getAllGenders() {
        List<GenderMaster> genders = genderRepository.findAll();
        return genders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get only active genders
     */
    public List<GenderMasterResponse> getActiveGenders() {
        List<GenderMaster> genders = genderRepository.findByIsActiveOrderByName(true);
        return genders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get gender by ID
     */
    public GenderMasterResponse getGenderById(Long id) {
        GenderMaster gender = genderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));
        
        return convertToResponse(gender);
    }

    /**
     * Create new gender entry
     */
    @Transactional
    public GenderMasterResponse createGender(GenderMasterRequest request) {
        // Check if gender with same name already exists
        if (genderRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Gender with name '" + request.getName() + "' already exists");
        }

        // Create new gender
        GenderMaster gender = GenderMaster.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        GenderMaster savedGender = genderRepository.save(gender);

        return GenderMasterResponse.builder()
                .id(savedGender.getId())
                .name(savedGender.getName())
                .description(savedGender.getDescription())
                .isActive(savedGender.getIsActive())
                .createdAt(savedGender.getCreatedAt())
                .status(true)
                .message("Gender created successfully")
                .build();
    }

    /**
     * Update existing gender
     */
    @Transactional
    public GenderMasterResponse updateGender(Long id, GenderMasterRequest request) {
        // Find existing gender
        GenderMaster gender = genderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));

        // Check if name is being changed to an existing name
        if (!gender.getName().equals(request.getName())) {
            if (genderRepository.findByName(request.getName()).isPresent()) {
                throw new RuntimeException("Gender with name '" + request.getName() + "' already exists");
            }
        }

        // Update fields
        gender.setName(request.getName());
        gender.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            gender.setIsActive(request.getIsActive());
        }

        GenderMaster updatedGender = genderRepository.save(gender);

        return GenderMasterResponse.builder()
                .id(updatedGender.getId())
                .name(updatedGender.getName())
                .description(updatedGender.getDescription())
                .isActive(updatedGender.getIsActive())
                .createdAt(updatedGender.getCreatedAt())
                .updatedAt(updatedGender.getUpdatedAt())
                .status(true)
                .message("Gender updated successfully")
                .build();
    }

    /**
     * Delete gender by ID
     */
    @Transactional
    public GenderMasterResponse deleteGender(Long id) {
        GenderMaster gender = genderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));

        // Perform hard delete
        genderRepository.deleteById(id);

        return GenderMasterResponse.builder()
                .id(gender.getId())
                .name(gender.getName())
                .status(true)
                .message("Gender deleted successfully")
                .build();
    }

    /**
     * Toggle active status
     */
    @Transactional
    public GenderMasterResponse toggleActiveStatus(Long id, Boolean isActive) {
        GenderMaster gender = genderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));

        gender.setIsActive(isActive);
        GenderMaster updatedGender = genderRepository.save(gender);

        return GenderMasterResponse.builder()
                .id(updatedGender.getId())
                .name(updatedGender.getName())
                .description(updatedGender.getDescription())
                .isActive(updatedGender.getIsActive())
                .updatedAt(updatedGender.getUpdatedAt())
                .status(true)
                .message("Gender status updated to " + (isActive ? "active" : "inactive"))
                .build();
    }

    /**
     * Convert entity to response DTO
     */
    private GenderMasterResponse convertToResponse(GenderMaster gender) {
        return GenderMasterResponse.builder()
                .id(gender.getId())
                .name(gender.getName())
                .description(gender.getDescription())
                .isActive(gender.getIsActive())
                .createdAt(gender.getCreatedAt())
                .updatedAt(gender.getUpdatedAt())
                .status(true)
                .message(null)
                .build();
    }
}
