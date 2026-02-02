package com.astro.backend.Repositry;

import com.astro.backend.Entity.DashaCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashaCalculationRepository extends JpaRepository<DashaCalculation, Long> {
    List<DashaCalculation> findByBirthChartId(Long birthChartId);
    Optional<DashaCalculation> findByBirthChartIdAndDashaType(Long birthChartId, String dashaType);
}
