package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RemidesPurchaseHistoryResponse {
    private Long id;
    private String orderId;
    private String orderType;
    private Long userId;
    private Long remidesId;
    private Long pujaId;
    private Long addressId;
    private String title;
    private String subtitle;
    private String imageBase64;
    private Integer totalItems;
    private Double unitPrice;
    private Double discountPercentage;
    private Double finalUnitPrice;
    private Double amount;
    private String currency;
    private String status;
    private LocalDateTime purchasedAt;
    private String paymentMethod;
    private String transactionId;
    private Double walletUsed;
    private Double gatewayPaid;
    private LocalDateTime slotTime;
}
