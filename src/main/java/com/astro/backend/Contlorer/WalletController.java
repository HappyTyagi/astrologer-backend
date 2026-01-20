package com.astro.backend.Contlorer;


import com.astro.backend.Entity.Wallet;
import com.astro.backend.RequestDTO.AddMoneyRequest;
import com.astro.backend.Services.RazorpayService;
import com.astro.backend.Services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final RazorpayService razorpayService;

    @GetMapping("/balance/{userId}")
    public Wallet getBalance(@PathVariable Long userId) {
        return walletService.getWallet(userId);
    }

    @PostMapping("/add-money/{userId}")
    public String createOrder(@PathVariable Long userId, @RequestBody AddMoneyRequest req) throws Exception {
        return razorpayService.createOrder(req.getAmount(), req.getCurrency());
    }

    @PostMapping("/transactions/{userId}")
    public Object getTransactions(@PathVariable Long userId) {
        return walletService.getWallet(userId);
    }
}
