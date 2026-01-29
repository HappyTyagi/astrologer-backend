package com.astro.backend.Services;

import com.astro.backend.Entity.AstrologerProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.AstrologerProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.AstrologerApprovalRequest;
import com.astro.backend.RequestDTO.AstrologerProfileRequest;
import com.astro.backend.RequestDTO.AstrologerSearchRequest;
import com.astro.backend.ResponseDTO.AstrologerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AstrologerProfileService {

    private final AstrologerProfileRepository astrologerProfileRepository;
    private final UserRepository userRepository;

    /**
     * Create astrologer profile
     */
    @Transactional
    public AstrologerProfileResponse createProfile(AstrologerProfileRequest request) {
        // Check if user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        // Check if profile already exists
        if (astrologerProfileRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("Astrologer profile already exists for this user");
        }

        // Create profile
        AstrologerProfile profile = AstrologerProfile.builder()
                .userId(request.getUserId())
                .consultationRate(request.getConsultationRate())
                .bio(request.getBio())
                .certifications(request.getCertifications())
                .languages(request.getLanguages())
                .specializations(request.getSpecializations())
                .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0)
                .maxConcurrentChats(request.getMaxConcurrentChats() != null ? request.getMaxConcurrentChats() : 3)
                .isAvailable(false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        AstrologerProfile savedProfile = astrologerProfileRepository.save(profile);

        return buildResponse(savedProfile, user, true, "Astrologer profile created successfully");
    }

    /**
     * Update astrologer profile
     */
    @Transactional
    public AstrologerProfileResponse updateProfile(Long profileId, AstrologerProfileRequest request) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields
        profile.setConsultationRate(request.getConsultationRate());
        profile.setBio(request.getBio());
        profile.setCertifications(request.getCertifications());
        profile.setLanguages(request.getLanguages());
        profile.setSpecializations(request.getSpecializations());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setMaxConcurrentChats(request.getMaxConcurrentChats());
        
        if (request.getIsAvailable() != null) {
            profile.setIsAvailable(request.getIsAvailable());
        }
        if (request.getIsActive() != null) {
            profile.setIsActive(request.getIsActive());
        }

        AstrologerProfile updatedProfile = astrologerProfileRepository.save(profile);

        return buildResponse(updatedProfile, user, true, "Profile updated successfully");
    }

    /**
     * Get astrologer profile by ID
     */
    public AstrologerProfileResponse getProfileById(Long profileId) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildResponse(profile, user, true, "Profile retrieved successfully");
    }

    /**
     * Get astrologer profile by user ID
     */
    public AstrologerProfileResponse getProfileByUserId(Long userId) {
        AstrologerProfile profile = astrologerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found for user"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildResponse(profile, user, true, "Profile retrieved successfully");
    }

    /**
     * Get all active astrologers
     */
    public List<AstrologerProfileResponse> getAllActiveAstrologers() {
        List<AstrologerProfile> profiles = astrologerProfileRepository.findByIsApprovedAndIsActive(true, true);
        return profiles.stream()
                .map(this::buildResponseWithUser)
                .collect(Collectors.toList());
    }

    /**
     * Get available astrologers
     */
    public List<AstrologerProfileResponse> getAvailableAstrologers() {
        List<AstrologerProfile> profiles = astrologerProfileRepository
                .findByIsAvailableAndIsActiveOrderByRatingDesc(true, true);
        return profiles.stream()
                .filter(p -> p.getIsApproved() != null && p.getIsApproved())
                .map(this::buildResponseWithUser)
                .collect(Collectors.toList());
    }

    /**
     * Search astrologers
     */
    public List<AstrologerProfileResponse> searchAstrologers(AstrologerSearchRequest request) {
        List<AstrologerProfile> profiles = astrologerProfileRepository.findAll();

        // Apply filters
        return profiles.stream()
                .filter(p -> p.getIsActive() && p.getIsApproved())
                .filter(p -> request.getSpecialization() == null || 
                        (p.getSpecializations() != null && p.getSpecializations().toLowerCase()
                                .contains(request.getSpecialization().toLowerCase())))
                .filter(p -> request.getLanguage() == null || 
                        (p.getLanguages() != null && p.getLanguages().toLowerCase()
                                .contains(request.getLanguage().toLowerCase())))
                .filter(p -> request.getMinRating() == null || 
                        (p.getRating() != null && p.getRating() >= request.getMinRating()))
                .filter(p -> request.getMaxRate() == null || 
                        (p.getConsultationRate() != null && p.getConsultationRate() <= request.getMaxRate()))
                .filter(p -> request.getAvailableOnly() == null || !request.getAvailableOnly() || 
                        (p.getIsAvailable() != null && p.getIsAvailable()))
                .sorted((a, b) -> {
                    if ("rate".equalsIgnoreCase(request.getSortBy())) {
                        return Double.compare(a.getConsultationRate(), b.getConsultationRate());
                    } else if ("experience".equalsIgnoreCase(request.getSortBy())) {
                        return Integer.compare(b.getExperienceYears() != null ? b.getExperienceYears() : 0,
                                a.getExperienceYears() != null ? a.getExperienceYears() : 0);
                    } else {
                        // Default: sort by rating
                        return Double.compare(b.getRating() != null ? b.getRating() : 0.0,
                                a.getRating() != null ? a.getRating() : 0.0);
                    }
                })
                .map(this::buildResponseWithUser)
                .collect(Collectors.toList());
    }

    /**
     * Toggle availability
     */
    @Transactional
    public AstrologerProfileResponse toggleAvailability(Long profileId, Boolean isAvailable) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setIsAvailable(isAvailable);
        if (isAvailable) {
            profile.setAvailableFrom(LocalDateTime.now());
        } else {
            profile.setAvailableTo(LocalDateTime.now());
        }

        AstrologerProfile updatedProfile = astrologerProfileRepository.save(profile);

        return buildResponse(updatedProfile, user, true, 
                "Availability updated to " + (isAvailable ? "available" : "unavailable"));
    }

    /**
     * Approve/Reject astrologer
     */
    @Transactional
    public AstrologerProfileResponse approveAstrologer(Long profileId, AstrologerApprovalRequest request) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setIsApproved(request.getIsApproved());
        profile.setApprovalNotes(request.getApprovalNotes());
        
        if (request.getIsApproved()) {
            profile.setApprovedAt(LocalDateTime.now());
        }

        AstrologerProfile updatedProfile = astrologerProfileRepository.save(profile);

        return buildResponse(updatedProfile, user, true, 
                request.getIsApproved() ? "Astrologer approved successfully" : "Astrologer rejected");
    }

    /**
     * Verify astrologer
     */
    @Transactional
    public AstrologerProfileResponse verifyAstrologer(Long profileId, Boolean isVerified) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setIsVerified(isVerified);

        AstrologerProfile updatedProfile = astrologerProfileRepository.save(profile);

        return buildResponse(updatedProfile, user, true, 
                isVerified ? "Astrologer verified successfully" : "Astrologer verification removed");
    }

    /**
     * Get pending approvals
     */
    public List<AstrologerProfileResponse> getPendingApprovals() {
        List<AstrologerProfile> profiles = astrologerProfileRepository.findAll();
        return profiles.stream()
                .filter(p -> p.getIsApproved() == null || !p.getIsApproved())
                .filter(p -> p.getIsActive())
                .map(this::buildResponseWithUser)
                .collect(Collectors.toList());
    }

    /**
     * Delete astrologer profile
     */
    @Transactional
    public AstrologerProfileResponse deleteProfile(Long profileId) {
        AstrologerProfile profile = astrologerProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Astrologer profile not found"));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete
        profile.setIsActive(false);
        astrologerProfileRepository.save(profile);

        return buildResponse(profile, user, true, "Astrologer profile deleted successfully");
    }

    // Helper methods

    private AstrologerProfileResponse buildResponseWithUser(AstrologerProfile profile) {
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return buildResponse(profile, user, true, null);
    }

    private AstrologerProfileResponse buildResponse(AstrologerProfile profile, User user, 
                                                    Boolean status, String message) {
        return AstrologerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .name(user != null ? user.getName() : null)
                .email(user != null ? user.getEmail() : null)
                .mobileNumber(user != null ? user.getMobileNumber() : null)
                .consultationRate(profile.getConsultationRate())
                .bio(profile.getBio())
                .certifications(profile.getCertifications())
                .rating(profile.getRating())
                .totalSessions(profile.getTotalSessions())
                .totalClients(profile.getTotalClients())
                .isAvailable(profile.getIsAvailable())
                .availableFrom(profile.getAvailableFrom())
                .availableTo(profile.getAvailableTo())
                .languages(profile.getLanguages())
                .maxConcurrentChats(profile.getMaxConcurrentChats())
                .specializations(profile.getSpecializations())
                .isVerified(profile.getIsVerified())
                .isApproved(profile.getIsApproved())
                .approvedAt(profile.getApprovedAt())
                .approvalNotes(profile.getApprovalNotes())
                .experienceYears(profile.getExperienceYears())
                .isActive(profile.getIsActive())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .status(status)
                .message(message)
                .build();
    }
}
