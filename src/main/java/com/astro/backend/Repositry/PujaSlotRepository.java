package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PujaSlotRepository extends JpaRepository<PujaSlot, Long> {
    List<PujaSlot> findByPujaIdAndStatus(Long pujaId, PujaSlot.SlotStatus status);
    List<PujaSlot> findByPujaIdOrderBySlotTimeAsc(Long pujaId);
    boolean existsByPujaIdAndSlotTime(Long pujaId, java.time.LocalDateTime slotTime);
    long countByPujaIdAndSlotTimeAfter(Long pujaId, LocalDateTime slotTime);
}
