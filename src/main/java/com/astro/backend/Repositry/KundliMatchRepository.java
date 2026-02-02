package com.astro.backend.Repositry;

import com.astro.backend.Entity.KundliMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KundliMatchRepository extends JpaRepository<KundliMatch, Long> {
    List<KundliMatch> findByBirthChart1Id(Long birthChart1Id);
    List<KundliMatch> findByBirthChart2Id(Long birthChart2Id);
    Optional<KundliMatch> findByBirthChart1IdAndBirthChart2Id(Long birthChart1Id, Long birthChart2Id);
}
