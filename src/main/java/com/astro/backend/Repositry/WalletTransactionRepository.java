package com.astro.backend.Repositry;

import com.astro.backend.Entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WalletTransaction> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String type,
            String status,
            LocalDateTime start,
            LocalDateTime end
    );
}
