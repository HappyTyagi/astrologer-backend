package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "puja_samagri_purchase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaSamagriPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "samagri_master_id", nullable = false)
    @JsonIgnore
    private PujaSamagriMaster samagriMaster;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    @JsonIgnore
    private Address address;

    @Column(nullable = false)
    private String name;

    private String hiName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;

    private Double discountPercentage;

    @Column(nullable = false)
    private Double finalUnitPrice;

    @Column(nullable = false)
    private Double lineTotal;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    private String paymentMethod;
    private String transactionId;
    private Double walletUsed;
    private Double gatewayPaid;

    @JsonProperty("samagriMasterId")
    public Long getSamagriMasterId() {
        return samagriMaster != null ? samagriMaster.getId() : null;
    }

    @JsonProperty("addressId")
    public Long getAddressId() {
        return address != null ? address.getId() : null;
    }

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (purchasedAt == null) {
            purchasedAt = LocalDateTime.now();
        }
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
        if (status == null || status.isBlank()) {
            status = "COMPLETED";
        }
    }
}
