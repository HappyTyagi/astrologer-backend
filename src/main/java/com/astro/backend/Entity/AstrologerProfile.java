package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "astrologer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;               // Foreign key to User

    private Double consultationRate;   // Per minute rate
    private String bio;                // Astrologer bio
    
    @Column(columnDefinition = "TEXT")
    private String certifications;     // JSON or comma-separated
    
    private Double rating;             // Average rating (0-5)
    private Integer totalSessions;     // Total completed sessions
    private Integer totalClients;      // Unique clients served

    private Boolean isAvailable;       // Current availability
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;

    private String languages;          // Languages spoken (comma-separated)
    private Integer maxConcurrentChats; // Max chats allowed simultaneously

    // ===== Specializations =====
    private String specializations;    // Vedic, Numerology, Tarot, Vastu, etc.
    
    // ===== CMS Management =====
    private Boolean isVerified;        // Email/Phone verification
    private Boolean isApproved;        // CMS admin approval
    private LocalDateTime approvedAt;
    private String approvalNotes;

    private Integer experienceYears;   // Years of experience
    private Boolean isActive;          // Account status

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isAvailable = false;
        isVerified = false;
        isApproved = false;
        isActive = true;
        totalSessions = 0;
        totalClients = 0;
        rating = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
