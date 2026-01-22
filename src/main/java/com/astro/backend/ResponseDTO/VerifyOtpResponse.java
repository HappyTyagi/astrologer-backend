package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private Boolean isValid;           // OTP verification status
    private String accessToken;        // JWT token if valid
    private String refreshToken;       // Refresh token if valid
    private String message;            // Status message
    private Long userId;               // User ID if valid
    private String name;               // User name if valid
    private String email;              // User email if valid
    private String mobileNumber;       // User mobile if valid
    private String role;               // User role if valid
    private Boolean isNewUser;         // Flag for new user
}

