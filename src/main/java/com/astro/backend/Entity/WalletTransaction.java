package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private double amount;
    private String type; // CREDIT / DEBIT
    private String refId;
    private String description;
    
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    // ===== Enhanced Transaction Tracking =====
    private String paymentGateway;     // Razorpay, Stripe, etc.
    private String status;             // SUCCESS, FAILED, PENDING
    private String failureReason;      // If transaction failed
    
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;
    
    private Long orderId;              // Order reference
    private String invoiceUrl;         // For receipts
    
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
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
