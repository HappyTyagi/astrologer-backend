package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaBookingRequest {
    private Long userId;
    private Long pujaId;
    private Long slotId;
    private Long addressId;
    private String paymentMethod; // WALLET / GATEWAY / UPI / CARD
    private String transactionId; // optional for gateway flows
    private Boolean useWallet;    // if true, force wallet debit
}
