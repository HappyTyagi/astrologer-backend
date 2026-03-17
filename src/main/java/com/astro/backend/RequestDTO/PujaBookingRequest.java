package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaBookingRequest {
    private Long userId;
    private Long pujaId;
    private Long slotId;
    private Long addressId;
    private Long gotraMasterId;
    private String customGotraName;
    private String paymentMethod; // WALLET / GATEWAY / UPI / CARD
    private String transactionId; // optional for gateway flows
    private Boolean useWallet;    // if true, force wallet debit
    private String packageCode;   // BASE / REGULAR / PREMIUM
    private String packageName;   // UI-visible package name
    private Double packagePrice;  // Selected package price
    private Integer packageDurationMinutes;
    private Long rashiMasterId;      // optional - auto picked from app/profile
    private Long nakshatraMasterId;  // optional - auto picked from app/profile
}
