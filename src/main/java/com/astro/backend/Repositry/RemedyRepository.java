package com.astro.backend.Repositry;

import com.astro.backend.Entity.Remedy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RemedyRepository extends JpaRepository<Remedy, Long> {
    List<Remedy> findByBirthChartId(Long birthChartId);
    List<Remedy> findByBirthChartIdAndRemedyType(Long birthChartId, String remedyType);
    List<Remedy> findByBirthChartIdAndRemedyFor(Long birthChartId, String remedyFor);
}
