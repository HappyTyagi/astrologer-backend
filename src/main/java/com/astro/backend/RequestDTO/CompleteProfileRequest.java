package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class CompleteProfileRequest {
    
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo;
    
    @NotBlank(message = "Full name is required")
    private String name;
    
    @NotBlank(message = "Date of birth is required (format: YYYY-MM-DD)")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "DOB must be in format YYYY-MM-DD")
    private String dob;
    
    @NotBlank(message = "Birth time is required (format: HH:MM)")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Birth time must be in format HH:MM")
    private String birthTime;
    
    @NotBlank(message = "AM/PM is required")
    @Pattern(regexp = "^(AM|PM)$", message = "amPm must be either AM or PM")
    private String amPm;
    
    @NotBlank(message = "Gender is required")
    private String gender;  // Male, Female, Other
    
    @NotNull(message = "State ID is required")
    private Long stateId;
    
    @NotNull(message = "District ID is required")
    private Long districtId;
    
    @NotNull(message = "Latitude is required")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    private Double longitude;
    
    @NotBlank(message = "Address is required")
    private String address;
}
