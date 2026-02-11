package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity to track birthday notifications for users
 * Creates notification template for upcoming birthdays
 */
@Entity
@Table(name = "birthday_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Foreign Keys =====
    @Column(name = "user_id", nullable = false)
    private Long userId;  // User whose birthday it is

    // ===== Birthday Details =====
    @Column(nullable = false)
    private String userFullName;

    private String userEmail;

    private String userMobileNumber;

    private String userProfileImage;

    private Integer upcomingYear;  // Year of birthday being celebrated (e.g., 2026)

    // ===== Notification Template =====
    @Column(columnDefinition = "TEXT")
    private String title;  // e.g., "Happy Birthday [Name]!"

    @Column(columnDefinition = "TEXT")
    private String message;  // Birthday greeting message

    @Column(columnDefinition = "TEXT")
    private String templateBody;  // Full template content

    private String templateImageUrl;  // Birthday greeting image/banner

    private String templateIconUrl;  // Special icon/emoji

    // ===== Delivery Status =====
    private Boolean emailSent;  // Whether email was sent

    private LocalDateTime emailSentAt;

    private Boolean appNotificationSent;  // Whether app notification was sent

    private LocalDateTime appNotificationSentAt;

    private Boolean isViewed;  // Whether user viewed in app

    private LocalDateTime viewedAt;

    // ===== Special Offers/Incentives =====
    private Double discountPercentage;  // Birthday discount offer

    private String discountCode;  // Coupon code for birthday discount

    private String offerDescription;  // e.g., "Get 20% off on consultations"

    private LocalDateTime offerValidTill;  // When the offer expires

    // ===== Management =====
    private Boolean isSent;  // Master flag: true if any notification sent

    private String status;  // PENDING, SENT, VIEWED, EXPIRED

    private String failureReason;  // If notification failed to send

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "PENDING";
        emailSent = false;
        appNotificationSent = false;
        isViewed = false;
        isSent = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
