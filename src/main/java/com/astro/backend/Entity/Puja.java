package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Puja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private double price;              // in INR
    private int durationMinutes;
    private String category;

    // ===== Enhanced Details =====
    private String image;              // Puja image URL (for mobile app)
    private String benefits;           // Benefits of puja
    private String rituals;            // Rituals involved
    private Integer minParticipants;   // Minimum participants
    private Integer maxParticipants;   // Maximum participants
    private Long astrologerId;         // Conducting astrologer

    // ===== CMS Management =====
    private String status;             // ACTIVE, INACTIVE, ARCHIVED
    private Integer viewCount;         // Track popularity
    private Boolean isFeatured;        // For mobile showcase
    private LocalDateTime featureExpiry;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
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
