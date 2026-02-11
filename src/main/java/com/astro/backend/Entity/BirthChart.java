package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "birth_charts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;  // Foreign key to User

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private Double birthTime;  // HH.MM format

    private Double latitude;

    private Double longitude;

    private String timezone;  // e.g., "Asia/Kolkata"

    // Calculated fields
    private String lagna;  // Ascendant/Rising sign

    private String sunSign;

    private String moonSign;

    @Column(columnDefinition = "TEXT")
    private String planetaryPositions;  // JSON string of all planets

    @Column(columnDefinition = "TEXT")
    private String housesData;  // JSON string of 12 houses

    private String nakshatra;  // Moon's nakshatra

    private String pada;  // Moon's pada

    @Column(columnDefinition = "TEXT")
    private String navamsaData;  // D9 chart JSON

    @Column(columnDefinition = "TEXT")
    private String dashamsa;  // D10 chart JSON

    // Doshas
    private Boolean hasMangalDosha;
    private Boolean hasKaalSarpDosha;
    private Boolean hasPitruDosha;
    private Boolean hasGrahanDosha;

    // Yogas
    @Column(columnDefinition = "TEXT")
    private String auspiciousYogas;  // JSON array

    @Column(columnDefinition = "TEXT")
    private String inauspiciousYogas;  // JSON array

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
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
