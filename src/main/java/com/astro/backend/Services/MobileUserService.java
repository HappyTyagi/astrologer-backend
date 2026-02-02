package com.astro.backend.Services;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.MobileUserRegistrationRequest;
import com.astro.backend.ResponseDTO.MobileUserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MobileUserService {

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;

    /**
     * Register new mobile user with device details
     */
    @Transactional
    public MobileUserProfileResponse registerMobileUser(MobileUserRegistrationRequest request) {
        try {
            // Check if mobile number already exists
            if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
                throw new RuntimeException("Mobile number already registered");
            }

            // Check if device is already registered
            if (mobileUserProfileRepository.findByDeviceId(request.getDeviceId()).isPresent()) {
                throw new RuntimeException("Device already registered");
            }

            // Create User entity
            User user = User.builder()
                    .name(request.getName())
                    .mobileNumber(request.getMobileNumber())
                    .email(request.getEmail())
                    .role(Role.USER)
                    .isVerified(false)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Save user first to get user ID
            User savedUser = userRepository.save(user);

            // Generate referral code if not provided
            String referralCode = request.getReferralCode();
            if (referralCode == null || referralCode.isEmpty()) {
                referralCode = "REF" + savedUser.getId() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            }

            // Create Mobile User Profile
            MobileUserProfile mobileProfile = MobileUserProfile.builder()
                    .userId(savedUser.getId())
                    .deviceToken(request.getDeviceToken())
                    .fcmToken(request.getFcmToken())
                    .deviceId(request.getDeviceId())
                    .appVersion(request.getAppVersion())
                    .osType(request.getOsType())
                    .dateOfBirth(request.getDateOfBirth())
                    .age(request.getAge())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .genderMasterId(request.getGenderMasterId())
                    .stateMasterId(request.getStateMasterId())
                    .districtMasterId(request.getDistrictMasterId())
                    .referralCode(referralCode)
                    .referredByUserId(request.getReferredByUserId())
                    .isProfileComplete(false)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            MobileUserProfile savedProfile = mobileUserProfileRepository.save(mobileProfile);

            // Build response
            return MobileUserProfileResponse.builder()
                    .status(true)
                    .message("Mobile user registered successfully")
                    .userId(savedUser.getId())
                    .name(savedUser.getName())
                    .mobileNumber(savedUser.getMobileNumber())
                    .email(savedUser.getEmail())
                    .isVerified(savedUser.getIsVerified())
                    .isActive(savedUser.getIsActive())
                    .mobileProfileId(savedProfile.getId())
                    .deviceToken(savedProfile.getDeviceToken())
                    .deviceId(savedProfile.getDeviceId())
                    .appVersion(savedProfile.getAppVersion())
                    .osType(savedProfile.getOsType())
                    .dateOfBirth(savedProfile.getDateOfBirth())
                    .age(savedProfile.getAge())
                    .latitude(savedProfile.getLatitude())
                    .longitude(savedProfile.getLongitude())
                    .genderMasterId(savedProfile.getGenderMasterId())
                    .stateMasterId(savedProfile.getStateMasterId())
                    .districtMasterId(savedProfile.getDistrictMasterId())
                    .profileImageUrl(savedProfile.getProfileImageUrl())
                    .isProfileComplete(savedProfile.getIsProfileComplete())
                    .referralCode(savedProfile.getReferralCode())
                    .referredByUserId(savedProfile.getReferredByUserId())
                    .createdAt(savedUser.getCreatedAt())
                    .lastLoginAt(savedProfile.getLastLoginAt())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to register mobile user: " + e.getMessage());
        }
    }

    /**
     * Get mobile user profile by user ID
     * If userId is null or invalid, it will throw an exception
     */
    public MobileUserProfileResponse getMobileUserProfile(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("User ID is required and must be greater than 0");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for user ID: " + userId));

        return buildResponse(user, profile, "Mobile user profile retrieved successfully");
    }

    /**
     * Get mobile user profile by MOBILE NUMBER (PRIMARY LOOKUP)
     * Supports mobile-number-first architecture
     */
    public MobileUserProfileResponse getMobileUserProfileByMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new RuntimeException("Mobile number is required");
        }
        
        MobileUserProfile profile = mobileUserProfileRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for mobile number: " + mobileNumber));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for userId: " + profile.getUserId()));

        return buildResponse(user, profile, "Mobile user profile retrieved successfully");
    }

    /**
     * Update mobile user device details by user ID
     */
    @Transactional
    public MobileUserProfileResponse updateDeviceDetails(Long userId, String deviceToken, 
            String fcmToken, String appVersion) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("User ID is required and must be greater than 0");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for user ID: " + userId));

        // Update device details
        profile.setDeviceToken(deviceToken);
        profile.setFcmToken(fcmToken);
        profile.setAppVersion(appVersion);
        profile.setLastLoginAt(LocalDateTime.now());

        MobileUserProfile updatedProfile = mobileUserProfileRepository.save(profile);

        return buildResponse(user, updatedProfile, "Device details updated successfully");
    }

    /**
     * Update mobile user device details by MOBILE NUMBER (PRIMARY LOOKUP)
     * Supports mobile-number-first architecture
     */
    @Transactional
    public MobileUserProfileResponse updateDeviceDetailsByMobileNumber(String mobileNumber, String deviceToken, 
            String fcmToken, String appVersion) {
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new RuntimeException("Mobile number is required");
        }
        
        MobileUserProfile profile = mobileUserProfileRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for mobile number: " + mobileNumber));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for userId: " + profile.getUserId()));

        // Update device details
        profile.setDeviceToken(deviceToken);
        profile.setFcmToken(fcmToken);
        profile.setAppVersion(appVersion);
        profile.setLastLoginAt(LocalDateTime.now());

        MobileUserProfile updatedProfile = mobileUserProfileRepository.save(profile);

        return buildResponse(user, updatedProfile, "Device details updated successfully");
    }

    /**
     * Helper method to build response
     */
    private MobileUserProfileResponse buildResponse(User user, MobileUserProfile profile, String message) {
        return MobileUserProfileResponse.builder()
                .status(true)
                .message(message)
                .userId(user.getId())
                .name(user.getName())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .mobileProfileId(profile.getId())
                .deviceToken(profile.getDeviceToken())
                .deviceId(profile.getDeviceId())
                .appVersion(profile.getAppVersion())
                .osType(profile.getOsType())
                .dateOfBirth(profile.getDateOfBirth())
                .age(profile.getAge())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .genderMasterId(profile.getGenderMasterId())
                .stateMasterId(profile.getStateMasterId())
                .districtMasterId(profile.getDistrictMasterId())
                .profileImageUrl(profile.getProfileImageUrl())
                .isProfileComplete(profile.getIsProfileComplete())
                .referralCode(profile.getReferralCode())
                .referredByUserId(profile.getReferredByUserId())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(profile.getLastLoginAt())
                .build();
    }
}
