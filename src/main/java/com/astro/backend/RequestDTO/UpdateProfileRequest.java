package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateProfileRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    private String name;  // Optional - user name

    private String mobileNo; // Optional - for logging only

    private String deviceToken; // Optional - device push token

    private String fcmToken; // Optional - FCM token

    private String deviceId; // Optional - device identifier

    private String appVersion; // Optional - app version

    private String osType; // Optional - OS type
    
    @NotBlank(message = "Date of Birth is required (format: YYYY-MM-DD)")
    private String dateOfBirth;  // Format: YYYY-MM-DD
    
    private String birthTime;  // Optional - Format: HH:mm
    
    private String amPm;  // Optional - AM or PM

    private String birthAmPm;  // Optional - AM or PM (alternate field name)

    private Integer age; // Optional - will be calculated from DOB
    
    @NotNull(message = "Gender Master ID is required")
    private Long genderMasterId;  // Foreign key to GenderMaster
    
    @NotNull(message = "State Master ID is required")
    private Long stateMasterId;  // Foreign key to StateMaster
    
    @NotNull(message = "District Master ID is required")
    private Long districtMasterId;  // Foreign key to DistrictMaster
    
    private Double latitude;    // Optional
    private Double longitude;   // Optional
    
    private String address;  // Optional - full address
}
