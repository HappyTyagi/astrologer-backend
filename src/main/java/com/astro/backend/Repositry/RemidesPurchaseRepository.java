package com.astro.backend.Repositry;

import com.astro.backend.Entity.RemidesPurchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    Optional<RemidesPurchase> findFirstByOrderIdOrderByIdDesc(String orderId);

    @Query(
            value = """
                    SELECT rp.order_id
                    FROM remides_purchase rp
                    WHERE UPPER(REPLACE(rp.order_id, '-', '')) LIKE CONCAT(UPPER(:compactPrefix), '%')
                    ORDER BY rp.id DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    String findLatestOrderIdByCompactPrefix(@Param("compactPrefix") String compactPrefix);
}
