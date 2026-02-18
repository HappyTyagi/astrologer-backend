package com.astro.backend.Repositry;

import com.astro.backend.Entity.GemstoneMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GemstoneMasterRepository extends JpaRepository<GemstoneMaster, Long> {
    List<GemstoneMaster> findByIsActiveOrderByName(Boolean isActive);
}
