package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSamagriItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PujaSamagriItemRepository extends JpaRepository<PujaSamagriItem, Long> {
    List<PujaSamagriItem> findByPujaIdAndIsActiveOrderByDisplayOrderAscIdAsc(Long pujaId, Boolean isActive);
    Optional<PujaSamagriItem> findByPujaIdAndSamagriMasterIdAndIsActive(Long pujaId, Long samagriMasterId, Boolean isActive);
}
