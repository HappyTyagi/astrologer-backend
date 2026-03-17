package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "puja_samagri_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaSamagriMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(unique = true)
    private String hiName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String hiDescription;

    private Double price;

    private Double discountPercentage;

    private Double finalPrice;

    private String currency;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean shopEnabled;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (shopEnabled == null) {
            shopEnabled = false;
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateFinalPrice();
    }

    @PreUpdate
    protected void onUpdate() {
        if (isActive == null) {
            isActive = true;
        }
        if (shopEnabled == null) {
            shopEnabled = false;
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
        updatedAt = LocalDateTime.now();
        calculateFinalPrice();
    }

    private void calculateFinalPrice() {
        if (price == null) {
            finalPrice = null;
            return;
        }
        if (discountPercentage != null && discountPercentage > 0) {
            finalPrice = price - (price * discountPercentage / 100);
        } else {
            finalPrice = price;
        }
        if (finalPrice != null && finalPrice < 0) {
            finalPrice = 0.0;
        }
    }
}
