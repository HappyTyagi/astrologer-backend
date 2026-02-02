package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kundli_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KundliMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long birthChart1Id;  // First person's birth chart

    @Column(nullable = false)
    private Long birthChart2Id;  // Second person's birth chart

    // Guna Milan (36 gun match)
    private Integer totalGunMatches;  // Out of 36

    private Integer varnaMilans;  // Caste (1 point)
    private Integer vasya_Milans;  // Control (2 points)
    private Integer taraaMilans;  // Star (3 points)
    private Integer yoniMilans;  // Nature (4 points)
    private Integer grihaMelakaMilans;  // Temperament (5 points)
    private Integer ganaMilans;  // Qualities (6 points)
    private Integer bhakootMilans;  // Relationship (7 points)
    private Integer naadiMilans;  // Health & constitution (8 points)

    // Doshas
    private Boolean hasNaadiDosha;  // Critical dosha
    private Boolean hasBhakootDosha;  // Relationship dosha
    private Boolean hasMangalDosha;  // Mangal affliction

    // Scores
    private Double compatibilityScore;  // Percentage 0-100
    private String compatibilityLevel;  // "Excellent", "Good", "Average", "Poor"

    @Column(columnDefinition = "TEXT")
    private String report;  // Detailed compatibility report

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
