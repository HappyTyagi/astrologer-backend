package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.Wallet;
import com.astro.backend.Entity.WalletTransaction;
import com.astro.backend.RequestDTO.AddMoneyRequest;
import com.astro.backend.Services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

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
        walletService.credit(
                userId,
                req.getAmount(),
                "WALLET_TOPUP",
                "Wallet top-up via app"
        );
        return walletService.getWallet(userId);
    }

    @GetMapping("/transactions/{userId}")
    public List<WalletTransaction> getTransactions(@PathVariable Long userId) {
        return walletService.getTransactions(userId);
    }
}
