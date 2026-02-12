package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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
    @Lob
    @Column(columnDefinition = "LONGTEXT")
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
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean popupEnabled;      // Popup visibility flag
    private LocalDate popupStartDate;  // Popup starts from this date
    private LocalDate popupEndDate;    // Popup valid till this date
    private Integer popupPriority;     // Higher priority first
    private String popupLabel;         // Optional badge text

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
        if (popupEnabled == null) {
            popupEnabled = true;
        }
        if (popupPriority == null) {
            popupPriority = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
