package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity to manage astrologer pricing based on district
 * Allows different astrologers to have different rates in different districts
 */
@Entity
@Table(name = "astrologer_district_price", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"astrologer_id", "district_master_id", "puja_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstrologerDistrictPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Foreign Keys =====
    @Column(name = "astrologer_id", nullable = false)
    private Long astrologerId;  // User ID of the astrologer

    @Column(name = "district_master_id", nullable = false)
    private Long districtMasterId;  // Foreign key to DistrictMaster

    @Column(name = "puja_id", nullable = false)
    private Long pujaId;  // Foreign key to Puja service

    // ===== Pricing Details =====
    @Column(nullable = false)
    private Double consultationPrice;  // Price per consultation/session in INR

    private Double discountPercentage;  // Optional discount for this district (0-100)

    private Double finalPrice;  // Calculated: consultationPrice - (consultationPrice * discountPercentage/100)

    // ===== Management =====
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;  // Enable/disable pricing for this combination

    private String notes;  // Additional notes (e.g., "Peak season rate", "Special offer")

    private Integer minBookings;  // Minimum bookings per week (for tracking)

    private Integer maxCapacity;  // Maximum slots available per week

    // ===== Timestamps =====
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime validFrom;  // When this price becomes effective

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime validTill;  // Price expiry date (null = indefinite)

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
        calculateFinalPrice();
        if (validFrom == null) {
            validFrom = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateFinalPrice();
    }

    /**
     * Calculate final price after applying discount
     */
    private void calculateFinalPrice() {
        if (consultationPrice != null && discountPercentage != null && discountPercentage > 0) {
            finalPrice = consultationPrice - (consultationPrice * discountPercentage / 100);
        } else {
            finalPrice = consultationPrice;
        }
    }
}
