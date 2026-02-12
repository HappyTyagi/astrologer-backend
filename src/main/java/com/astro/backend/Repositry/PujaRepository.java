package com.astro.backend.Repositry;


import com.astro.backend.Entity.Puja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PujaRepository extends JpaRepository<Puja, Long> {
    List<Puja> findByIsActiveTrue();
    Optional<Puja> findByNameIgnoreCase(String name);
    List<Puja> findByPopupEndDateBeforeAndIsActiveTrue(LocalDate date);
    List<Puja> findByStatusIgnoreCaseAndIsActiveTrue(String status);
}
