package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileUserProfileResponse {

    private Boolean status;
    private String message;

    // ===== User Details =====
    private Long userId;
    private String name;
    private String mobileNumber;
    private String email;
    private Boolean isVerified;
    private Boolean isActive;

    // ===== Mobile Profile Details =====
    private Long mobileProfileId;
    private String deviceToken;
    private String deviceId;
    private String appVersion;
    private String osType;

    // ===== Profile Information =====
    private String dateOfBirth;
    private Integer age;
    private Double latitude;
    private Double longitude;
    private Long genderMasterId;
    private Long stateMasterId;
    private Long districtMasterId;
    private String profileImageUrl;
    private Boolean isProfileComplete;

    // ===== Language Preference =====
    private Integer isLanguage; // 1 = English, 2 = Hindi

    // ===== Referral =====
    private String referralCode;
    private Integer referredByUserId;

    // ===== Timestamps =====
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
