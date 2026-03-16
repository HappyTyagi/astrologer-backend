package com.astro.backend.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class PujaSamagriPurchaseRequest {
    private Long userId;
    private Long addressId;
    private String paymentMethod; // WALLET / GATEWAY / UPI / CARD
    private String transactionId; // optional for gateway
    private Boolean useWallet;    // use wallet (full/partial) with gateway
    private List<PurchaseItem> items;

    @Data
    public static class PurchaseItem {
        private Long samagriMasterId;
        private Integer quantity;
    }
}
