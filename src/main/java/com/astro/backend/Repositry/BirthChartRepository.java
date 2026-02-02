package com.astro.backend.Repositry;

import com.astro.backend.Entity.BirthChart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BirthChartRepository extends JpaRepository<BirthChart, Long> {
    Optional<BirthChart> findByUserId(Long userId);
    List<BirthChart> findAllByUserId(Long userId);
}
