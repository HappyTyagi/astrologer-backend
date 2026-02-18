package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "mobile_user_profiles",
    indexes = {
        @Index(name = "idx_mobile_user_profiles_mobile_number", columnList = "mobileNumber")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileUserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Foreign Key Reference to User Table =====
    @Column(nullable = false, unique = true)
    private Long userId;

    // ===== Basic Details =====
    @Column(nullable = false)
    private String name;
    private String mobileNumber;
    private String email;

    // ===== Mobile Device Management (Optional - populated on first login) =====
    private String deviceToken;        // For push notifications
    private String fcmToken;           // Firebase Cloud Messaging
    private String deviceId;           // Unique device identifier
    private String appVersion;         // Current app version
    private String osType;             // iOS, Android

    // ===== Profile & Location Details =====
    private String dateOfBirth;        // For kundli services
    private String birthTime;          // Format: HH:MM (e.g., 14:30)
    private String birthAmPm;          // AM or PM
    private Integer age;
    private Double latitude;
    private Double longitude;
    private Double mobileLatitude;
    private Double mobileLongitude;
    private String address;            // Full address
    private Boolean isMarried;         // Marital status
    private String anniversaryDate;    // Format: YYYY-MM-DD

    // ===== Master Data Foreign Keys =====
    private Long genderMasterId;       // Foreign key to GenderMaster
    private Long stateMasterId;        // Foreign key to StateMaster
    private Long districtMasterId;     // Foreign key to DistrictMaster
    private Long gemstoneMasterId;     // Foreign key to GemstoneMaster (optional)
    private Long yantraMasterId;       // Foreign key to YantraMaster (optional)

    // ===== Profile Management =====
    private Boolean isProfileComplete; // Track profile completeness
    private LocalDateTime lastLoginAt;
    private String profileImageUrl;

    // ===== Language Preference =====
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer isLanguage;        // 1 = English, 2 = Hindi

    // ===== Referral System =====
    private String referralCode;       // For referral system
    private Integer referredByUserId;  // Track referrals

    // ===== Timestamps =====
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (isLanguage == null) {
            isLanguage = 1; // Default English
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
