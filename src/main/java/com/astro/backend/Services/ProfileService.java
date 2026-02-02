package com.astro.backend.Services;

import com.astro.backend.Entity.GenderMaster;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.GenderMasterRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.CompleteProfileRequest;
import com.astro.backend.RequestDTO.UpdateProfileRequest;
import com.astro.backend.ResponseDTO.UpdateProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final GenderMasterRepository genderMasterRepository;

    /**
     * Get existing user profile by userId - for auto-fill form
     * Returns all profile data stored in database
     */
    public UpdateProfileResponse getProfileByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("User ID is required and must be greater than 0");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Optional<MobileUserProfile> profileOpt = mobileUserProfileRepository.findByUserId(userId);
        
        if (profileOpt.isEmpty()) {
            // User exists but no profile yet - return basic user info
            return UpdateProfileResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .mobileNumber(user.getMobileNumber())
                    .status(true)
                    .message("User found but profile not completed yet")
                    .build();
        }

        MobileUserProfile profile = profileOpt.get();
        
        // Return all profile data for auto-fill
        return UpdateProfileResponse.builder()
                .userId(user.getId())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .age(profile.getAge())
                .genderMasterId(profile.getGenderMasterId())
                .stateMasterId(profile.getStateMasterId())
                .districtMasterId(profile.getDistrictMasterId())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .status(true)
                .message("Profile data retrieved successfully")
                .build();
    }

    /**
     * Get existing user profile by mobile number - for auto-fill form
     * Returns all profile data stored in database
     */
    public UpdateProfileResponse getProfileByMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new RuntimeException("Mobile number is required");
        }
        
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found with mobile number: " + mobileNumber));

        Optional<MobileUserProfile> profileOpt = mobileUserProfileRepository.findByMobileNumber(mobileNumber);
        
        if (profileOpt.isEmpty()) {
            // User exists but no profile yet - return basic user info
            return UpdateProfileResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .mobileNumber(user.getMobileNumber())
                    .status(true)
                    .message("User found but profile not completed yet")
                    .build();
        }

        MobileUserProfile profile = profileOpt.get();
        
        // Return all profile data for auto-fill
        return UpdateProfileResponse.builder()
                .userId(user.getId())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .age(profile.getAge())
                .genderMasterId(profile.getGenderMasterId())
                .stateMasterId(profile.getStateMasterId())
                .districtMasterId(profile.getDistrictMasterId())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .status(true)
                .message("Profile data retrieved successfully")
                .build();
    }

    /**
     * Update user profile - creates User + MobileUserProfile if doesn't exist
     * PRIMARY LOOKUP: Mobile number (if provided)
     * FALLBACK LOOKUP: userId (for backward compatibility)
     */
    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        
        Long userId = request.getUserId();
        String mobileNo = request.getMobileNo();
        MobileUserProfile mobileProfile = null;
        User user = null;

        // STEP 1: Try to find user by MOBILE NUMBER (PRIMARY LOOKUP)
        if (mobileNo != null && !mobileNo.isEmpty()) {
            // Look up by mobile number first in MobileUserProfile
            Optional<MobileUserProfile> existingProfile = mobileUserProfileRepository.findByMobileNumber(mobileNo);
            if (existingProfile.isPresent()) {
                // User already exists with this mobile number
                mobileProfile = existingProfile.get();
                userId = mobileProfile.getUserId();
                final Long finalUserId = userId;
                user = userRepository.findById(finalUserId)
                        .orElseThrow(() -> new RuntimeException("User not found for userId: " + finalUserId));
            } else {
                // Check if user exists in User table (may not have MobileUserProfile yet)
                Optional<User> existingUser = userRepository.findByMobileNumber(mobileNo);
                if (existingUser.isPresent()) {
                    // User exists but no MobileUserProfile - link them
                    user = existingUser.get();
                    userId = user.getId();
                    
                    // Create new MobileUserProfile for this existing user
                    mobileProfile = MobileUserProfile.builder()
                            .userId(userId)
                            .name(request.getName() != null ? request.getName() : user.getName())
                            .mobileNumber(mobileNo)
                            .isProfileComplete(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                } else {
                    // Completely new user - create both user and profile
                    user = User.builder()
                            .name(request.getName() != null ? request.getName() : "User")
                            .mobileNumber(mobileNo)
                            .isVerified(true)
                            .isActive(true)
                            .role(com.astro.backend.EnumFile.Role.USER)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    user = userRepository.save(user);
                    userId = user.getId();
                    
                    // Create new MobileUserProfile
                    final Long newUserId = userId;
                    mobileProfile = MobileUserProfile.builder()
                            .userId(newUserId)
                            .name(request.getName() != null ? request.getName() : "User")
                            .mobileNumber(mobileNo)
                            .isProfileComplete(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                }
            }
        } 
        // STEP 2: Fallback to userId lookup if mobile number not provided
        else if (userId != null && userId > 0) {
            final Long lookupUserId = userId;
            user = userRepository.findById(lookupUserId)
                    .orElseThrow(() -> new RuntimeException("User not found for user ID: " + lookupUserId));
            
            // Find or create MobileUserProfile by userId
            Optional<MobileUserProfile> existingProfile = mobileUserProfileRepository.findByUserId(lookupUserId);
            if (existingProfile.isPresent()) {
                mobileProfile = existingProfile.get();
            } else {
                // Create new MobileUserProfile if it doesn't exist
                mobileProfile = MobileUserProfile.builder()
                        .userId(lookupUserId)
                        .name(request.getName() != null ? request.getName() : user.getName())
                        .mobileNumber(user.getMobileNumber())
                        .isProfileComplete(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
        }
        // STEP 3: Error if neither mobile number nor userId provided
        else {
            throw new RuntimeException("Either mobileNo or userId is required");
        }

        // Update user name if provided
        if (request.getName() != null && !request.getName().isEmpty() && user != null) {
            user.setName(request.getName());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        try {
            // Parse and validate DOB
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dob = LocalDate.parse(request.getDateOfBirth(), formatter);
            
            // Calculate age
            Integer age = calculateAge(dob);

            // Update mobile profile fields
            if (request.getName() != null && !request.getName().isEmpty()) {
                mobileProfile.setName(request.getName());
            }
            if (request.getMobileNo() != null && !request.getMobileNo().isEmpty()) {
                mobileProfile.setMobileNumber(request.getMobileNo());
            }
            mobileProfile.setDateOfBirth(request.getDateOfBirth());
            mobileProfile.setAge(age);
            mobileProfile.setGenderMasterId(request.getGenderMasterId());
            mobileProfile.setStateMasterId(request.getStateMasterId());
            mobileProfile.setDistrictMasterId(request.getDistrictMasterId());
            mobileProfile.setIsProfileComplete(true);

            // Update device/app details if provided
            if (request.getDeviceToken() != null && !request.getDeviceToken().isEmpty()) {
                mobileProfile.setDeviceToken(request.getDeviceToken());
            }
            if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
                mobileProfile.setFcmToken(request.getFcmToken());
            }
            if (request.getDeviceId() != null && !request.getDeviceId().isEmpty()) {
                mobileProfile.setDeviceId(request.getDeviceId());
            }
            if (request.getAppVersion() != null && !request.getAppVersion().isEmpty()) {
                mobileProfile.setAppVersion(request.getAppVersion());
            }
            if (request.getOsType() != null && !request.getOsType().isEmpty()) {
                mobileProfile.setOsType(request.getOsType());
            }

            // Save birth time if provided
            if (request.getBirthTime() != null && !request.getBirthTime().isEmpty()) {
                mobileProfile.setBirthTime(request.getBirthTime());
            }
            String amPm = request.getBirthAmPm() != null ? request.getBirthAmPm() : request.getAmPm();
            if (amPm != null && !amPm.isEmpty()) {
                mobileProfile.setBirthAmPm(amPm);
            }

            // Save location if provided
            if (request.getLatitude() != null && request.getLongitude() != null) {
                mobileProfile.setLatitude(request.getLatitude());
                mobileProfile.setLongitude(request.getLongitude());
            }

            // Save address if provided
            if (request.getAddress() != null && !request.getAddress().isEmpty()) {
                mobileProfile.setAddress(request.getAddress());
            }

            mobileProfile.setUpdatedAt(LocalDateTime.now());

            // Save to mobile_user_profiles only
            MobileUserProfile updatedProfile = mobileUserProfileRepository.save(mobileProfile);

            // Build and return response (data comes from mobile profile only)
                return UpdateProfileResponse.builder()
                    .userId(updatedProfile.getUserId())
                    .name(updatedProfile.getName() != null ? updatedProfile.getName() : "User")
                    .email(updatedProfile.getEmail())
                    .mobileNumber(updatedProfile.getMobileNumber())
                    .dateOfBirth(updatedProfile.getDateOfBirth())
                    .age(updatedProfile.getAge())
                    .genderMasterId(updatedProfile.getGenderMasterId())
                    .stateMasterId(updatedProfile.getStateMasterId())
                    .districtMasterId(updatedProfile.getDistrictMasterId())
                    .latitude(updatedProfile.getLatitude())
                    .longitude(updatedProfile.getLongitude())
                    .birthTime(updatedProfile.getBirthTime())
                    .birthAmPm(updatedProfile.getBirthAmPm())
                    .address(updatedProfile.getAddress())
                    .status(true)
                    .message("Profile updated successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error updating profile: " + e.getMessage());
        }
    }

    /**
     * Calculate age from date of birth
     */
    private Integer calculateAge(LocalDate dateOfBirth) {
        LocalDate today = LocalDate.now();
        return Period.between(dateOfBirth, today).getYears();
    }

    /**
     * Complete user profile with all required details after OTP verification
     * Creates MobileUserProfile if doesn't exist, updates if exists
     */
    @Transactional
    public UpdateProfileResponse completeProfile(CompleteProfileRequest request) {
        // Find user by mobile number
        User user = userRepository.findByMobileNumber(request.getMobileNo())
                .orElseThrow(() -> new RuntimeException("User not found with mobile number: " + request.getMobileNo()));

        // Update user name
        user.setName(request.getName());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        try {
            // Parse and validate DOB
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dob = LocalDate.parse(request.getDob(), formatter);
            
            // Calculate age
            Integer age = calculateAge(dob);

            // Find gender master by name
            Long genderMasterId = null;
            Optional<GenderMaster> genderOpt = genderMasterRepository.findByName(request.getGender());
            if (genderOpt.isPresent()) {
                genderMasterId = genderOpt.get().getId();
            }

            // Check if mobile user profile exists
            Optional<MobileUserProfile> profileOpt = mobileUserProfileRepository.findByUserId(savedUser.getId());
            MobileUserProfile mobileProfile;

            if (profileOpt.isPresent()) {
                // Update existing profile
                mobileProfile = profileOpt.get();
            } else {
                // Create new profile
                mobileProfile = MobileUserProfile.builder()
                        .userId(savedUser.getId())
                    .name(request.getName())
                    .mobileNumber(request.getMobileNo())
                        .deviceToken("") // Will be updated later
                        .fcmToken("")
                        .deviceId("")
                        .appVersion("1.0")
                        .osType("Unknown")
                        .build();
            }

            // Update profile fields
            mobileProfile.setName(request.getName());
            mobileProfile.setMobileNumber(request.getMobileNo());
            mobileProfile.setDateOfBirth(request.getDob());
            mobileProfile.setBirthTime(request.getBirthTime());
            mobileProfile.setBirthAmPm(request.getAmPm());
            mobileProfile.setAge(age);
            mobileProfile.setGenderMasterId(genderMasterId);
            mobileProfile.setStateMasterId(request.getStateId());
            mobileProfile.setDistrictMasterId(request.getDistrictId());
            mobileProfile.setLatitude(request.getLatitude());
            mobileProfile.setLongitude(request.getLongitude());
            mobileProfile.setIsProfileComplete(true);
            mobileProfile.setUpdatedAt(LocalDateTime.now());

            if (profileOpt.isEmpty()) {
                mobileProfile.setCreatedAt(LocalDateTime.now());
            }

            MobileUserProfile savedProfile = mobileUserProfileRepository.save(mobileProfile);

            // Build and return response
            return UpdateProfileResponse.builder()
                    .userId(savedUser.getId())
                    .name(savedUser.getName())
                    .email(savedUser.getEmail())
                    .mobileNumber(savedUser.getMobileNumber())
                    .dateOfBirth(savedProfile.getDateOfBirth())
                    .age(savedProfile.getAge())
                    .genderMasterId(savedProfile.getGenderMasterId())
                    .stateMasterId(savedProfile.getStateMasterId())
                    .districtMasterId(savedProfile.getDistrictMasterId())
                    .latitude(savedProfile.getLatitude())
                    .longitude(savedProfile.getLongitude())
                    .status(true)
                    .message("Profile completed successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error completing profile: " + e.getMessage());
        }
    }
}
