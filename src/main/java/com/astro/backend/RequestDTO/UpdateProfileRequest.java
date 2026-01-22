package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateProfileRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Date of Birth is required (format: YYYY-MM-DD)")
    private String dateOfBirth;  // Format: YYYY-MM-DD
    
    @NotNull(message = "Gender Master ID is required")
    private Long genderMasterId;  // Foreign key to GenderMaster
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotNull(message = "State Master ID is required")
    private Long stateMasterId;  // Foreign key to StateMaster
    
    @NotNull(message = "District Master ID is required")
    private Long districtMasterId;  // Foreign key to DistrictMaster
    
    private Double latitude;    // Optional
    private Double longitude;   // Optional
}
