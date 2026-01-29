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
}

