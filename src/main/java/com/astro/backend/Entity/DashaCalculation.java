package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dasha_calculations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashaCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long birthChartId;  // Foreign key to BirthChart

    @Column(nullable = false)
    private String dashaType;  // "Vimshottari", "Yogini", "Char"

    private String mahadashaLord;  // Planet ruling Mahadasha

    private LocalDate mahadashaStart;
    private LocalDate mahadashaEnd;
    private Integer mahadashaYears;

    private String antardashaLord;  // Planet ruling Antardasha

    private LocalDate antardashaStart;
    private LocalDate antardashaEnd;

    private String pratyantardashaLord;  // Pratyantar Dasha

    private LocalDate pratyantardashaStart;
    private LocalDate pratyantardashaEnd;

    @Column(columnDefinition = "TEXT")
    private String dashaTimeline;  // JSON array of all dasha periods

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
