package com.astro.backend.Repositry;

import com.astro.backend.Entity.PanchangData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PanchangDataRepository extends JpaRepository<PanchangData, Long> {
    Optional<PanchangData> findByDate(LocalDate date);
}
