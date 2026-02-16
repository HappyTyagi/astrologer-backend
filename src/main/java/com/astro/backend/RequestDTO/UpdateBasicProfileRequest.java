package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBasicProfileRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    private String name;
    private String email;
    private String emailOtpSessionId;
    private String dateOfBirth; // Supports YYYY-MM-DD or DD/MM/YYYY
    private String birthTime;   // Optional - Format: HH:mm
    private String birthAmPm;   // Optional - AM or PM
    private Long genderMasterId;
    private Long stateMasterId;
    private Long districtMasterId;
    private String address;
    private Boolean isMarried;
    private String anniversaryDate; // Format: YYYY-MM-DD
}
