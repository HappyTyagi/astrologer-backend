package com.astro.backend.Repositry;

import com.astro.backend.Entity.RashiMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RashiMasterRepository extends JpaRepository<RashiMaster, Long> {
    List<RashiMaster> findByIsActiveOrderByName(Boolean isActive);
}
