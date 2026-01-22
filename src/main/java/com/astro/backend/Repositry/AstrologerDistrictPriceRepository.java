package com.astro.backend.Repositry;

import com.astro.backend.Entity.AstrologerDistrictPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AstrologerDistrictPriceRepository extends JpaRepository<AstrologerDistrictPrice, Long> {

    /**
     * Find price for specific astrologer, district, and puja
     */
    @Query("SELECT adp FROM AstrologerDistrictPrice adp " +
           "WHERE adp.astrologerId = :astrologerId " +
           "AND adp.districtMasterId = :districtMasterId " +
           "AND adp.pujaId = :pujaId " +
           "AND adp.isActive = true " +
           "AND (adp.validFrom <= :currentTime OR adp.validFrom IS NULL) " +
           "AND (adp.validTill >= :currentTime OR adp.validTill IS NULL)")
    Optional<AstrologerDistrictPrice> findActivePrice(
            @Param("astrologerId") Long astrologerId,
            @Param("districtMasterId") Long districtMasterId,
            @Param("pujaId") Long pujaId,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all prices for a specific astrologer
     */
    List<AstrologerDistrictPrice> findByAstrologerIdAndIsActiveTrue(Long astrologerId);

    /**
     * Find all prices for a specific district
     */
    List<AstrologerDistrictPrice> findByDistrictMasterIdAndIsActiveTrue(Long districtMasterId);

    /**
     * Find all prices for a specific puja
     */
    List<AstrologerDistrictPrice> findByPujaIdAndIsActiveTrue(Long pujaId);

    /**
     * Find prices for astrologer in specific district
     */
    List<AstrologerDistrictPrice> findByAstrologerIdAndDistrictMasterIdAndIsActiveTrue(
            Long astrologerId, Long districtMasterId);

    /**
     * Find expired or upcoming prices (for admin management)
     */
    @Query("SELECT adp FROM AstrologerDistrictPrice adp " +
           "WHERE adp.astrologerId = :astrologerId " +
           "AND (adp.validTill < :currentTime OR adp.validFrom > :currentTime)")
    List<AstrologerDistrictPrice> findExpiredOrUpcomingPrices(
            @Param("astrologerId") Long astrologerId,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Check if price mapping already exists
     */
    boolean existsByAstrologerIdAndDistrictMasterIdAndPujaId(
            Long astrologerId, Long districtMasterId, Long pujaId);
}
