package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 10, message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "Reference number is required")
    private String refNumber;
}
