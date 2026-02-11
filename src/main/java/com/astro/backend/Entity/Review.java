package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_astrologer", columnList = "astrologerId"),
    @Index(name = "idx_user", columnList = "userId"),
    @Index(name = "idx_booking", columnList = "bookingId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;            // Link to booking/session

    @Column(nullable = false)
    private Long userId;               // Who reviewed

    @Column(nullable = false)
    private Long astrologerId;         // Reviewed astrologer/entity

    @Column(nullable = false)
    private Integer rating;            // 1-5 stars

    @Column(columnDefinition = "TEXT")
    private String review;             // Review text

    private String reviewType;         // SERVICE_QUALITY, ACCURACY, PROFESSIONALISM, COMMUNICATION

    // ===== Community Features =====
    private Integer helpfulCount;      // Helpful votes
    private Integer unhelpfulCount;    // Unhelpful votes

    private Boolean isVerifiedPurchase; // Purchase verification

    // ===== CMS Management =====
    private Boolean isApproved;        // Moderation approval
    private Boolean isHidden;          // Hidden by admin
    private String hiddenReason;       // Why hidden

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        helpfulCount = 0;
        unhelpfulCount = 0;
        isApproved = false;
        isHidden = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
