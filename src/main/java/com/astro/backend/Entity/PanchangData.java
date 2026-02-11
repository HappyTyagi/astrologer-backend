package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "panchang_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanchangData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private String tithi;  // Lunar day

    private Double tithiEndTime;  // When current tithi ends

    private String nakshatra;  // Star position

    private Double nakshatraEndTime;

    private String yoga;  // Auspicious combination

    private String karana;  // Half tithi

    private String rahu_kaal_start;  // Inauspicious time period

    private String rahu_kaal_end;

    private String yamagandam_start;  // Another inauspicious period

    private String yamagandam_end;

    private String sunrise;  // Time of sunrise

    private String sunset;  // Time of sunset

    private String moonrise;

    private String moonset;

    private Boolean isShubhMuhurat;  // Is this day auspicious?

    @Column(columnDefinition = "TEXT")
    private String auspiciousTimings;  // JSON of auspicious times for events

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
