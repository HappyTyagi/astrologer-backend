package com.astro.backend.Repositry;

import com.astro.backend.Entity.YantraMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YantraMasterRepository extends JpaRepository<YantraMaster, Long> {
    List<YantraMaster> findByIsActiveOrderByName(Boolean isActive);
}
