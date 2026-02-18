package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.lang.Math;

@Entity
@Table(name = "remides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remides {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    private Double discountPercentage;

    private Double finalPrice;

    private Double tokenAmount;

    @Column(nullable = false)
    private String currency;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imageBase64;

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
        if (currency == null || currency.isEmpty()) {
            currency = "INR";
        }
        calculateFinalPrice();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (currency == null || currency.isEmpty()) {
            currency = "INR";
        }
        calculateFinalPrice();
    }

    private void calculateFinalPrice() {
        if (price == null) {
            finalPrice = null;
            tokenAmount = null;
            return;
        }
        if (discountPercentage != null && discountPercentage > 0) {
            finalPrice = price - (price * discountPercentage / 100);
        } else {
            finalPrice = price;
        }
        double minimumToken = Math.max(500.0, finalPrice * 0.10);
        minimumToken = Math.min(minimumToken, finalPrice);

        if (tokenAmount == null || tokenAmount <= 0) {
            tokenAmount = minimumToken;
            return;
        }

        if (tokenAmount < minimumToken) {
            tokenAmount = minimumToken;
        }
        if (tokenAmount > finalPrice) {
            tokenAmount = finalPrice;
        }
    }
}
