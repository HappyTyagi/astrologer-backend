package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 20)
    private String orderType; // REMEDY or PUJA

    @Column(nullable = false)
    private Long userId;

    private Long sourceId; // remides_purchase.id or puja_booking.id

    private Long remidesId;

    private Long pujaId;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imageBase64;

    @Column(nullable = false)
    private Integer totalItems;

    @Column(nullable = false)
    private Double unitPrice;

    private Double discountPercentage;

    @Column(nullable = false)
    private Double finalUnitPrice;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

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
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (purchasedAt == null) {
            purchasedAt = LocalDateTime.now();
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
