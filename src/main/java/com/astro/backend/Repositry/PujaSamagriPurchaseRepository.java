package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSamagriPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PujaSamagriPurchaseRepository extends JpaRepository<PujaSamagriPurchase, Long> {
    List<PujaSamagriPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    List<PujaSamagriPurchase> findByUserIdAndPurchasedAtAfterOrderByPurchasedAtDesc(Long userId, LocalDateTime since);

    List<PujaSamagriPurchase> findByOrderIdOrderByIdAsc(String orderId);
}
