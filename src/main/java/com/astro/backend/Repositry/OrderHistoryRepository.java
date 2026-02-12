package com.astro.backend.Repositry;

import com.astro.backend.Entity.OrderHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntry, Long> {
    List<OrderHistoryEntry> findByUserIdOrderByPurchasedAtDesc(Long userId);
    List<OrderHistoryEntry> findByUserIdAndPurchasedAtAfterOrderByPurchasedAtDesc(Long userId, LocalDateTime since);
    boolean existsByOrderTypeAndSourceId(String orderType, Long sourceId);
}
