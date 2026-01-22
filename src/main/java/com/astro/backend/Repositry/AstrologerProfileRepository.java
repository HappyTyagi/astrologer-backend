package com.astro.backend.Repositry;

import com.astro.backend.Entity.AstrologerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AstrologerProfileRepository extends JpaRepository<AstrologerProfile, Long> {

    Optional<AstrologerProfile> findByUserId(Long userId);

    List<AstrologerProfile> findByIsApprovedAndIsActive(Boolean isApproved, Boolean isActive);

    List<AstrologerProfile> findByIsAvailableAndIsActiveOrderByRatingDesc(Boolean isAvailable, Boolean isActive);

    @Query("SELECT ap FROM AstrologerProfile ap WHERE ap.rating >= :minRating AND ap.isActive = true ORDER BY ap.rating DESC")
    List<AstrologerProfile> findTopRatedAstrologers(@Param("minRating") Double minRating);

    @Query("SELECT ap FROM AstrologerProfile ap WHERE ap.isAvailable = true AND ap.isApproved = true ORDER BY ap.rating DESC LIMIT :limit")
    List<AstrologerProfile> findAvailableAstrologers(@Param("limit") int limit);

    List<AstrologerProfile> findBySpecializationsContainingAndIsActiveOrderByRatingDesc(String specialization, Boolean isActive);

    Integer countByIsApprovedAndIsActive(Boolean isApproved, Boolean isActive);
}
