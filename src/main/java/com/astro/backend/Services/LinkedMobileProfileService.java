package com.astro.backend.Services;

import com.astro.backend.Entity.LinkedMobileProfile;
import com.astro.backend.Repositry.LinkedMobileProfileRepository;
import com.astro.backend.RequestDTO.LinkedProfileCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LinkedMobileProfileService {

    private final LinkedMobileProfileRepository linkedMobileProfileRepository;

    @Transactional(readOnly = true)
    public List<LinkedMobileProfile> listByMobile(String mobileNo) {
        if (mobileNo == null || mobileNo.isBlank()) {
            throw new RuntimeException("mobileNo is required");
        }
        return linkedMobileProfileRepository
                .findByMobileNoAndIsActiveTrueOrderByIsPrimaryDescUpdatedAtDesc(mobileNo.trim());
    }

    @Transactional
    public Map<String, Object> create(LinkedProfileCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final String mobileNo = request.getMobileNo() == null ? "" : request.getMobileNo().trim();
        if (mobileNo.isEmpty()) {
            throw new RuntimeException("mobileNo is required");
        }
        final String profileName = request.getProfileName() == null ? "" : request.getProfileName().trim();
        if (profileName.isEmpty()) {
            throw new RuntimeException("profileName is required");
        }

        final boolean hasAny = linkedMobileProfileRepository
                .findFirstByMobileNoAndIsActiveTrueOrderByIsPrimaryDescUpdatedAtDesc(mobileNo)
                .isPresent();

        final boolean makePrimary = request.getMakePrimary() != null
                ? request.getMakePrimary()
                : !hasAny;

        if (makePrimary) {
            linkedMobileProfileRepository.clearPrimaryByMobileNo(mobileNo);
        }

        LinkedMobileProfile row = LinkedMobileProfile.builder()
                .userId(request.getUserId())
                .mobileNo(mobileNo)
                .profileName(profileName)
                .email(request.getEmail())
                .dateOfBirth(request.getDateOfBirth())
                .birthTime(request.getBirthTime())
                .birthAmPm(request.getBirthAmPm())
                .genderMasterId(request.getGenderMasterId())
                .stateMasterId(request.getStateMasterId())
                .districtMasterId(request.getDistrictMasterId())
                .address(request.getAddress())
                .isPrimary(makePrimary)
                .isActive(true)
                .build();
        LinkedMobileProfile saved = linkedMobileProfileRepository.save(row);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Linked profile created");
        response.put("profile", saved);
        return response;
    }

    @Transactional
    public Map<String, Object> makePrimary(String mobileNo, Long profileId) {
        if (mobileNo == null || mobileNo.isBlank()) {
            throw new RuntimeException("mobileNo is required");
        }
        if (profileId == null || profileId <= 0) {
            throw new RuntimeException("Valid profileId is required");
        }
        final String normalizedMobileNo = mobileNo.trim();
        LinkedMobileProfile target = linkedMobileProfileRepository
                .findByIdAndMobileNoAndIsActiveTrue(profileId, normalizedMobileNo)
                .orElseThrow(() -> new RuntimeException("Linked profile not found"));

        linkedMobileProfileRepository.clearPrimaryByMobileNo(normalizedMobileNo);
        target.setIsPrimary(true);
        linkedMobileProfileRepository.save(target);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Primary profile updated");
        response.put("profileId", target.getId());
        return response;
    }
}

