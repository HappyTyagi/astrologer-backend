package com.astro.backend.Services;

import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.UpdateProfileRequest;
import com.astro.backend.ResponseDTO.UpdateProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    /**
     * Update user profile with DOB, gender, city, state, and optional location
     * Calculates age from DOB and saves to database
     */
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        // Find user by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        try {
            // Parse and validate DOB
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dob = LocalDate.parse(request.getDateOfBirth(), formatter);
            
            // Calculate age
            Integer age = calculateAge(dob);

            // Update user fields
            user.setDateOfBirth(request.getDateOfBirth());
            user.setGenderMasterId(request.getGenderMasterId());
            user.setCity(request.getCity());
            user.setStateMasterId(request.getStateMasterId());
            user.setDistrictMasterId(request.getDistrictMasterId());
            user.setAge(age);
            user.setIsProfileComplete(true);

            // Save location if provided
            if (request.getLatitude() != null && request.getLongitude() != null) {
                user.setLatitude(request.getLatitude());
                user.setLongitude(request.getLongitude());
            }

            // Save updated user to database
            User updatedUser = userRepository.save(user);

            // Build and return response
            return UpdateProfileResponse.builder()
                    .userId(updatedUser.getId())
                    .name(updatedUser.getName())
                    .email(updatedUser.getEmail())
                    .mobileNumber(updatedUser.getMobileNumber())
                    .dateOfBirth(updatedUser.getDateOfBirth())
                    .age(updatedUser.getAge())
                    .genderMasterId(updatedUser.getGenderMasterId())
                    .city(updatedUser.getCity())
                    .stateMasterId(updatedUser.getStateMasterId())
                    .districtMasterId(updatedUser.getDistrictMasterId())
                    .latitude(updatedUser.getLatitude())
                    .longitude(updatedUser.getLongitude())
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
}
