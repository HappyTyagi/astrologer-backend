package com.astro.backend.Repositry;

import com.astro.backend.Entity.RemidesPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RemidesPurchaseRepository extends JpaRepository<RemidesPurchase, Long> {
    @EntityGraph(attributePaths = {"remides", "address"})
    List<RemidesPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"remides", "address"})
    List<RemidesPurchase> findByUserIdAndPurchasedAtAfterOrderByPurchasedAtDesc(
            Long userId,
            LocalDateTime purchasedAt
    );

    @EntityGraph(attributePaths = {"remides", "address"})
    List<RemidesPurchase> findByOrderIdOrderByIdAsc(String orderId);
}
