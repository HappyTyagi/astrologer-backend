package com.astro.backend.Services;


import com.astro.backend.Entity.Wallet;
import com.astro.backend.Entity.WalletTransaction;
import com.astro.backend.Repositry.WalletRepository;
import com.astro.backend.Repositry.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;

    public Wallet getWallet(Long userId) {
        return walletRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet w = Wallet.builder()
                            .userId(userId)
                            .balance(0)
                            .cashback(0.0)
                            .bonus(0.0)
                            .build();
                    return walletRepo.save(w);
                });
    }

    public void credit(Long userId, double amount, String ref, String desc) {
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepo.save(wallet);

        txnRepo.save(WalletTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type("CREDIT")
                .refId(ref)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public boolean debit(Long userId, double amount, String ref, String desc) {
        Wallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) return false;

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepo.save(wallet);

        txnRepo.save(WalletTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .type("DEBIT")
                .refId(ref)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build());

        return true;
    }
}
