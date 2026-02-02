package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private Boolean success;           // OTP verification status
    private String message;            // Status message
    private String token;              // JWT access token
    private String refreshToken;       // JWT refresh token
    private Long userId;               // User ID
    private String name;               // User name
    private String mobileNo;           // User mobile number
    private String email;              // User email
    private Boolean isNewUser;         // Whether user was just created
    private Boolean isProfileComplete; // Whether user profile is complete (has MobileUserProfile with complete data)
    
    // Profile data fields (for SharedPreferences storage in mobile)
    private String dateOfBirth;        // User's date of birth (YYYY-MM-DD)
    private Integer age;               // Calculated age
    private Long genderMasterId;       // Gender master ID
    private Long stateMasterId;        // State master ID
    private Long districtMasterId;     // District master ID
    private Double latitude;           // Location latitude
    private Double longitude;          // Location longitude
    private String birthTime;          // Birth time (HH:mm)
    private String birthAmPm;          // Birth time AM/PM
    private String address;            // Full address
}

