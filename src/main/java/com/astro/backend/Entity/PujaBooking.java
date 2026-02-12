package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PujaBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long pujaId;
    private Long slotId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @JsonIgnore
    private Address address;

    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    // ===== Enhanced Tracking =====
    private Double totalPrice;         // Final price after discounts
    private Double discountApplied;
    private String discountCode;
    private String paymentMethod;      // UPI, Card, Wallet, etc.
    private String transactionId;      // Payment gateway transaction ID
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // ===== For Mobile App & CMS =====
    private Integer rating;            // User rating 1-5
    private String review;             // User review
    private String notificationStatus; // For reminder tracking
    private LocalDateTime reminderSentAt;

    @JsonProperty("addressId")
    public Long getAddressId() {
        return address != null ? address.getId() : null;
    }

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        bookedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED, REFUNDED
    }
}
