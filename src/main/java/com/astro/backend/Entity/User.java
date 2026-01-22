package com.astro.backend.Entity;


import com.astro.backend.EnumFile.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Basic Details =====
    @Column(nullable = false)
    private String name;

    private String email;

    @Column(unique = true, nullable = false, length = 10)
    private String mobileNumber;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // ASTROLOGER, ADMIN

    private Integer age;

    private Double latitude;

    private Double longitude;

    private Boolean isVerified;

    private String country;

    private String city;

    private String profileImageUrl;

    private Boolean isActive;

    // ===== Mobile App & Device Management =====
    private String deviceToken;        // For push notifications
    private String fcmToken;           // Firebase Cloud Messaging
    private String deviceId;           // Unique device identifier
    private String appVersion;         // Current app version
    private String osType;             // iOS, Android, Web

    private LocalDateTime lastLoginAt;
    private Boolean isProfileComplete; // Track profile completeness

    // ===== Astrologer-Specific (CMS Management) =====
    private Integer yearsOfExperience; // For astrologers
    private String specialization;     // Numerology, Vastu, Vedic
    private Double rating;             // Average rating
    private Integer totalConsultations; // Consultation count
    private String certificateUrl;     // For CMS verification
    private Boolean isApproved;        // CMS admin approval
    private LocalDateTime approvedAt;
    private String approvalReason;     // Admin notes

    // ===== Profile Management =====
    private String dateOfBirth;        // For kundli services
    private String additionalPhone;    // Secondary contact
    private String referralCode;       // For referral system
    private Integer referredByUserId;  // Track referrals

    // ===== Master Data Foreign Keys =====
    private Long genderMasterId;       // Foreign key to GenderMaster
    private Long stateMasterId;        // Foreign key to StateMaster
    private Long districtMasterId;     // Foreign key to DistrictMaster

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
