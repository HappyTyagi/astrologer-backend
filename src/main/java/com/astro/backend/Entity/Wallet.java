package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private double balance;

    // ===== Enhanced Wallet =====
    private Double cashback;           // Accumulated cashback
    private Double bonus;              // Promotional bonus

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastUpdatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    private Boolean isActive;          // For CMS to deactivate

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
        isActive = true;
        cashback = 0.0;
        bonus = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
