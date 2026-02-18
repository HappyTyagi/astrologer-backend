package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "puja_booking_spiritual_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaBookingSpiritualDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonIgnore
    private PujaBooking booking;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long gotraMasterId;

    @Column(nullable = true)
    private Long rashiMasterId;

    @Column(nullable = true)
    private Long nakshatraMasterId;

    @Column(nullable = false)
    private String gotraName;

    @Column(nullable = true)
    private String rashiName;

    @Column(nullable = true)
    private String nakshatraName;

    @Column(nullable = false, updatable = false)
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
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
