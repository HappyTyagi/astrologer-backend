package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private Integer emailChangeCount;
    private Boolean emailChangeAllowed;
    private String mobileNumber;
    private String dateOfBirth;
    private Integer age;
    private Long genderMasterId;
    private String city;
    private Long stateMasterId;
    private Long districtMasterId;
    private Long gemstoneMasterId;
    private Long yantraMasterId;
    private Double latitude;
    private Double longitude;
    private Double mobileLatitude;
    private Double mobileLongitude;
    private String birthTime;          // Birth time (HH:mm)
    private String birthAmPm;          // Birth time AM/PM
    private String address;            // Full address
    private Boolean isMarried;         // Marital status
    private String anniversaryDate;    // Format: YYYY-MM-DD
    private Boolean status;
    private String message;
}
