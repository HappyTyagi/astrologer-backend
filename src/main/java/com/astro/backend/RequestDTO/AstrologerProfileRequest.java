package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerProfileRequest {

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Consultation rate is required")
    @Min(value = 0, message = "Consultation rate must be positive")
    private Double consultationRate;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    private String certifications;

    @Size(max = 500, message = "Languages must not exceed 500 characters")
    private String languages;

    @Size(max = 500, message = "Specializations must not exceed 500 characters")
    private String specializations;

    @Min(value = 0, message = "Experience years must be positive")
    private Integer experienceYears;

    @Min(value = 1, message = "Max concurrent chats must be at least 1")
    @Max(value = 10, message = "Max concurrent chats cannot exceed 10")
    private Integer maxConcurrentChats;

    private Boolean isAvailable;
    private Boolean isActive;
}
