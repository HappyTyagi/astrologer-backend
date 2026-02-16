package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileUserRegistrationRequest {

    // ===== User Basic Details =====
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    private String email;

    // ===== Mobile Device Details (Mandatory) =====
    @NotBlank(message = "Device token is required")
    private String deviceToken;

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "App version is required")
    private String appVersion;

    @NotBlank(message = "OS type is required (iOS/Android)")
    private String osType;

    // ===== Optional Profile Details =====
    private String dateOfBirth;
    private Integer age;
    private Double latitude;
    private Double longitude;
    private Long genderMasterId;
    private Long stateMasterId;
    private Long districtMasterId;
    private String profileImageUrl;
    private String referralCode;
    private Integer referredByUserId;
}
