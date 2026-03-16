package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSamagriCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PujaSamagriCartRepository extends JpaRepository<PujaSamagriCart, Long> {
    List<PujaSamagriCart> findByUserId(Long userId);

    List<PujaSamagriCart> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    Optional<PujaSamagriCart> findByUserIdAndSamagriMaster_IdAndIsActiveTrue(Long userId, Long samagriMasterId);
}
