package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResendReceiptRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}
