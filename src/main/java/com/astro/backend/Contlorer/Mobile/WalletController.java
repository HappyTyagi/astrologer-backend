package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.Wallet;
import com.astro.backend.Entity.WalletTransaction;
import com.astro.backend.RequestDTO.AddMoneyRequest;
import com.astro.backend.RequestDTO.WalletPaymentAttemptRequest;
import com.astro.backend.Services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {
    private static final double MIN_TOPUP_AMOUNT = 100.0;

    private final WalletService walletService;

    @GetMapping("/balance/{userId}")
    public Wallet getBalance(@PathVariable Long userId) {
        return walletService.getWallet(userId);
    }

    @PostMapping("/add-money/{userId}")
    public Wallet addMoney(@PathVariable Long userId, @RequestBody AddMoneyRequest req) throws Exception {
        if (req == null || req.getAmount() <= 0) {
            throw new RuntimeException("Valid amount is required");
        }
        if (req.getAmount() < MIN_TOPUP_AMOUNT) {
            throw new RuntimeException("Minimum wallet top-up is INR 100");
        }
        String referenceId = "TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        walletService.credit(
                userId,
                req.getAmount(),
                referenceId,
                "Wallet top-up via app"
        );
        return walletService.getWallet(userId);
    }

    @GetMapping("/transactions/{userId}")
    public List<WalletTransaction> getTransactions(@PathVariable Long userId) {
        return walletService.getTransactions(userId);
    }

    @PostMapping("/transactions/{userId}/attempt")
    public WalletTransaction logPaymentAttempt(
            @PathVariable Long userId,
            @RequestBody WalletPaymentAttemptRequest request
    ) {
        return walletService.logGatewayAttempt(
                userId,
                request == null ? null : request.getAmount(),
                request == null ? null : request.getReferenceId(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getPaymentGateway(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getFailureReason()
        );
    }
}
