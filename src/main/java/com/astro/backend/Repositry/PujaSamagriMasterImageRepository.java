package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSamagriMasterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PujaSamagriMasterImageRepository extends JpaRepository<PujaSamagriMasterImage, Long> {

    List<PujaSamagriMasterImage> findBySamagriMaster_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long samagriMasterId);

    List<PujaSamagriMasterImage> findBySamagriMaster_IdOrderByDisplayOrderAscIdAsc(Long samagriMasterId);

    @Query("""
            SELECT i FROM PujaSamagriMasterImage i
            WHERE i.isActive = true AND i.samagriMaster.id IN :ids
            ORDER BY i.samagriMaster.id ASC, i.displayOrder ASC, i.id ASC
            """)
    List<PujaSamagriMasterImage> findActiveByMasterIdsOrdered(@Param("ids") List<Long> ids);
}

