package com.astro.backend.Services;

import com.astro.backend.Entity.GenderMaster;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.GenderMasterRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.CompleteProfileRequest;
import com.astro.backend.RequestDTO.DeleteAccountRequest;
import com.astro.backend.RequestDTO.UpdateBasicProfileRequest;
import com.astro.backend.RequestDTO.UpdateEmailRequest;
import com.astro.backend.RequestDTO.UpdateProfileRequest;
import com.astro.backend.ResponseDTO.UpdateProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final GenderMasterRepository genderMasterRepository;

    private int getEmailChangeCount(User user) {
        return user.getEmailChangeCount() == null ? 0 : user.getEmailChangeCount();
    }

    private boolean isEmailChangeAllowed(User user) {
        return getEmailChangeCount(user) < 1;
    }

    private void updateEmailWithLimit(User user, MobileUserProfile profile, String emailInput) {
        if (emailInput == null || emailInput.isBlank()) {
            throw new RuntimeException("Valid email is required");
        }

        String newEmail = emailInput.trim();
        String existingEmail = user.getEmail() == null ? null : user.getEmail().trim();
        boolean hasExistingEmail = existingEmail != null && !existingEmail.isBlank();
        boolean isDifferentEmail = !Objects.equals(
                existingEmail == null ? null : existingEmail.toLowerCase(),
                newEmail.toLowerCase()
        );

        if (isDifferentEmail && hasExistingEmail && !isEmailChangeAllowed(user)) {
            throw new RuntimeException("Email can be changed only once");
        }

        if (isDifferentEmail) {
            user.setEmail(newEmail);
            if (hasExistingEmail) {
                user.setEmailChangeCount(getEmailChangeCount(user) + 1);
            }
        }

        if (profile != null) {
            profile.setEmail(user.getEmail());
        }
    }

    @Transactional
    public UpdateProfileResponse updateBasicProfile(UpdateBasicProfileRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> MobileUserProfile.builder()
                        .userId(user.getId())
                        .name(user.getName() != null ? user.getName() : "User")
                        .mobileNumber(user.getMobileNumber())
                        .isProfileComplete(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            user.setName(name);
            profile.setName(name);
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            updateEmailWithLimit(user, profile, request.getEmail());
        }

        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress().trim());
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            String normalizedDob = normalizeDob(request.getDateOfBirth().trim());
            profile.setDateOfBirth(normalizedDob);
            profile.setAge(calculateAge(LocalDate.parse(normalizedDob, DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        }

        if (request.getBirthTime() != null && !request.getBirthTime().isBlank()) {
            profile.setBirthTime(request.getBirthTime().trim());
        }
        if (request.getBirthAmPm() != null && !request.getBirthAmPm().isBlank()) {
            profile.setBirthAmPm(request.getBirthAmPm().trim());
        }
        if (request.getGenderMasterId() != null) {
            profile.setGenderMasterId(request.getGenderMasterId());
        }
        if (request.getStateMasterId() != null) {
            profile.setStateMasterId(request.getStateMasterId());
        }
        if (request.getDistrictMasterId() != null) {
            profile.setDistrictMasterId(request.getDistrictMasterId());
        }

        user.setUpdatedAt(LocalDateTime.now());
        profile.setMobileNumber(profile.getMobileNumber() == null ? user.getMobileNumber() : profile.getMobileNumber());
        profile.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        MobileUserProfile savedProfile = mobileUserProfileRepository.save(profile);

        return UpdateProfileResponse.builder()
                .userId(user.getId())
                .name(savedProfile.getName() != null ? savedProfile.getName() : user.getName())
                .email(user.getEmail())
                .emailChangeCount(getEmailChangeCount(user))
                .emailChangeAllowed(isEmailChangeAllowed(user))
                .mobileNumber(user.getMobileNumber())
                .dateOfBirth(savedProfile.getDateOfBirth())
                .age(savedProfile.getAge())
                .genderMasterId(savedProfile.getGenderMasterId())
                .stateMasterId(savedProfile.getStateMasterId())
                .districtMasterId(savedProfile.getDistrictMasterId())
                .birthTime(savedProfile.getBirthTime())
                .birthAmPm(savedProfile.getBirthAmPm())
                .address(savedProfile.getAddress())
                .status(true)
                .message("Basic profile updated successfully")
                .build();
    }

    @Transactional
    public UpdateProfileResponse updateEmail(UpdateEmailRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Valid email is required");
        }

        final User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));
        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(user.getId()).orElse(null);
        updateEmailWithLimit(user, profile, request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        if (profile != null) {
            profile.setUpdatedAt(LocalDateTime.now());
            mobileUserProfileRepository.save(profile);
        }

        return UpdateProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .emailChangeCount(getEmailChangeCount(user))
                .emailChangeAllowed(isEmailChangeAllowed(user))
                .mobileNumber(user.getMobileNumber())
                .status(true)
                .message("Email updated successfully")
                .build();
    }

    @Transactional
    public UpdateProfileResponse deleteAccount(DeleteAccountRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        user.setPromotionalNotificationsEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return UpdateProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .emailChangeCount(getEmailChangeCount(user))
                .emailChangeAllowed(isEmailChangeAllowed(user))
                .mobileNumber(user.getMobileNumber())
                .status(true)
                .message("Account marked deleted for promotions. Promotional notifications are disabled permanently.")
                .build();
    }

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
                    .emailChangeCount(getEmailChangeCount(user))
                    .emailChangeAllowed(isEmailChangeAllowed(user))
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
                .emailChangeCount(getEmailChangeCount(user))
                .emailChangeAllowed(isEmailChangeAllowed(user))
                .mobileNumber(user.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .age(profile.getAge())
                .genderMasterId(profile.getGenderMasterId())
                .stateMasterId(profile.getStateMasterId())
                .districtMasterId(profile.getDistrictMasterId())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .birthTime(profile.getBirthTime())
                .birthAmPm(profile.getBirthAmPm())
                .address(profile.getAddress())
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
                    .emailChangeCount(getEmailChangeCount(user))
                    .emailChangeAllowed(isEmailChangeAllowed(user))
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
                .emailChangeCount(getEmailChangeCount(user))
                .emailChangeAllowed(isEmailChangeAllowed(user))
                .mobileNumber(user.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .age(profile.getAge())
                .genderMasterId(profile.getGenderMasterId())
                .stateMasterId(profile.getStateMasterId())
                .districtMasterId(profile.getDistrictMasterId())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .birthTime(profile.getBirthTime())
                .birthAmPm(profile.getBirthAmPm())
                .address(profile.getAddress())
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
                    .email(user != null ? user.getEmail() : updatedProfile.getEmail())
                    .emailChangeCount(user != null ? getEmailChangeCount(user) : 0)
                    .emailChangeAllowed(user == null || isEmailChangeAllowed(user))
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

    private String normalizeDob(String input) {
        try {
            if (input.contains("/")) {
                LocalDate date = LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            LocalDate date = LocalDate.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid dateOfBirth format. Use YYYY-MM-DD or DD/MM/YYYY");
        }
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
                    .emailChangeCount(getEmailChangeCount(savedUser))
                    .emailChangeAllowed(isEmailChangeAllowed(savedUser))
                    .mobileNumber(savedUser.getMobileNumber())
                    .dateOfBirth(savedProfile.getDateOfBirth())
                    .age(savedProfile.getAge())
                    .genderMasterId(savedProfile.getGenderMasterId())
                    .stateMasterId(savedProfile.getStateMasterId())
                    .districtMasterId(savedProfile.getDistrictMasterId())
                    .latitude(savedProfile.getLatitude())
                    .longitude(savedProfile.getLongitude())
                    .birthTime(savedProfile.getBirthTime())
                    .birthAmPm(savedProfile.getBirthAmPm())
                    .address(savedProfile.getAddress())
                    .status(true)
                    .message("Profile completed successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error completing profile: " + e.getMessage());
        }
    }
}
