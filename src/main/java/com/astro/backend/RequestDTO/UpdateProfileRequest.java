package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class UpdateProfileRequest {
    
    private Long userId;  // Optional - will be looked up via mobile number if mobileNo is provided
    
    private String name;  // Optional - user name

    @NotBlank(message = "Mobile number is required (primary lookup key)")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo; // REQUIRED - primary identifier for user lookup

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
    private Double mobileLatitude;    // Optional
    private Double mobileLongitude;   // Optional
    
    private String address;  // Optional - full address
    private Boolean isMarried; // Optional - marital status
    private String anniversaryDate; // Optional - format: YYYY-MM-DD
}
