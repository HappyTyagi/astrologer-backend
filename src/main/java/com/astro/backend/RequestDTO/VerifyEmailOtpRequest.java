package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyEmailOtpRequest {
    @NotBlank(message = "email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "otp is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "sessionId is required")
    private String sessionId;
}

