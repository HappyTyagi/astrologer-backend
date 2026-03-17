package com.astro.backend.ResponseDTO;

import com.astro.backend.Entity.PujaSamagriPurchase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PujaSamagriPurchaseResponse {
    private String orderId;
    private Long userId;
    private Long addressId;
    private Integer totalItems;
    private Double totalAmount;
    private Double payableAmount;
    private String paymentMethod;
    private String transactionId;
    private Double walletUsed;
    private Double gatewayPaid;
    private LocalDateTime purchasedAt;
    private List<PujaSamagriPurchase> purchases;
}

