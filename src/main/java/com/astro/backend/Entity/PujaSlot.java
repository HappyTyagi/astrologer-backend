package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pujaId;
    private LocalDateTime slotTime;

    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    // ===== Enhanced Slot Management =====
    private Long astrologerId;         // Conducting astrologer
    private Integer maxBookings;       // Max bookings for slot
    private Integer currentBookings;   // Current booking count

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    private Boolean isRecurring;       // For recurring slots
    private String recurringPattern;   // DAILY, WEEKLY, MONTHLY
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        currentBookings = 0;
    }

    public enum SlotStatus {
        AVAILABLE, BOOKED, EXPIRED, CANCELLED
    }
}
