package com.astro.backend.Repositry;

import com.astro.backend.Entity.LinkedMobileProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkedMobileProfileRepository extends JpaRepository<LinkedMobileProfile, Long> {

    List<LinkedMobileProfile> findByMobileNoAndIsActiveTrueOrderByIsPrimaryDescUpdatedAtDesc(String mobileNo);

    Optional<LinkedMobileProfile> findByIdAndMobileNoAndIsActiveTrue(Long id, String mobileNo);

    Optional<LinkedMobileProfile> findFirstByMobileNoAndIsActiveTrueOrderByIsPrimaryDescUpdatedAtDesc(String mobileNo);

    @Modifying
    @Query("update LinkedMobileProfile p set p.isPrimary = false where p.mobileNo = :mobileNo and p.isActive = true")
    int clearPrimaryByMobileNo(@Param("mobileNo") String mobileNo);
}

