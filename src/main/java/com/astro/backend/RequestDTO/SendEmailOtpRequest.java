package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailOtpRequest {
    @NotBlank(message = "email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}

