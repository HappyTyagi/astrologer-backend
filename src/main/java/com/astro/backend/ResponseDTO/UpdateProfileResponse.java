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
    private String mobileNumber;
    private String dateOfBirth;
    private Integer age;
    private Long genderMasterId;
    private String city;
    private Long stateMasterId;
    private Long districtMasterId;
    private Double latitude;
    private Double longitude;
    private Boolean status;
    private String message;
}
