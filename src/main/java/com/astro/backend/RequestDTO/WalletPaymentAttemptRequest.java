package com.astro.backend.RequestDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletPaymentAttemptRequest {
    private Double amount;
    private String referenceId;
    private String description;
    private String paymentGateway;
    private String status; // FAILED / CANCELLED / PENDING
    private String failureReason;
}

