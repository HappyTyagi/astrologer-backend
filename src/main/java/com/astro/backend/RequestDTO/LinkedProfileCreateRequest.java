package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkedProfileCreateRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "mobileNo is required")
    private String mobileNo;

    @NotBlank(message = "profileName is required")
    private String profileName;

    private String email;
    private String dateOfBirth;
    private String birthTime;
    private String birthAmPm;
    private Long genderMasterId;
    private Long stateMasterId;
    private Long districtMasterId;
    private String address;
    private Boolean makePrimary;
}

