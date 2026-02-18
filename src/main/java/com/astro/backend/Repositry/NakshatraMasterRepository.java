package com.astro.backend.Repositry;

import com.astro.backend.Entity.NakshatraMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NakshatraMasterRepository extends JpaRepository<NakshatraMaster, Long> {
    List<NakshatraMaster> findByIsActiveOrderByName(Boolean isActive);
}
