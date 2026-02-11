package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBasicProfileRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    private String name;
    private String dateOfBirth; // Supports YYYY-MM-DD or DD/MM/YYYY
    private String address;
}
