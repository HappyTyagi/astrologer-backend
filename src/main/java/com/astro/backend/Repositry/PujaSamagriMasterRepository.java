package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaSamagriMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PujaSamagriMasterRepository extends JpaRepository<PujaSamagriMaster, Long> {
    List<PujaSamagriMaster> findByIsActiveOrderByName(Boolean isActive);
}
