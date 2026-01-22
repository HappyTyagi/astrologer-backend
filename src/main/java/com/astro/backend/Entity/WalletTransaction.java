package com.astro.backend.Entity;


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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

