package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mobile_user_profiles")
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
    private String address;            // Full address

    // ===== Master Data Foreign Keys =====
    private Long genderMasterId;       // Foreign key to GenderMaster
    private Long stateMasterId;        // Foreign key to StateMaster
    private Long districtMasterId;     // Foreign key to DistrictMaster

    // ===== Profile Management =====
    private Boolean isProfileComplete; // Track profile completeness
    private LocalDateTime lastLoginAt;
    private String profileImageUrl;

    // ===== Referral System =====
    private String referralCode;       // For referral system
    private Integer referredByUserId;  // Track referrals

    // ===== Timestamps =====
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
