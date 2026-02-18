package com.astro.backend.Repositry;

import com.astro.backend.Entity.GotraMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GotraMasterRepository extends JpaRepository<GotraMaster, Long> {
    List<GotraMaster> findByIsActiveOrderByName(Boolean isActive);
}
