package com.astro.backend.Repositry;

import com.astro.backend.Entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByAstrologerIdAndIsApprovedAndIsHiddenOrderByCreatedAtDesc(Long astrologerId, Boolean isApproved, Boolean isHidden);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Review> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.astrologerId = :astrologerId AND r.isApproved = true AND r.isHidden = false")
    Double getAverageRatingForAstrologer(@Param("astrologerId") Long astrologerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.astrologerId = :astrologerId AND r.isApproved = true AND r.isHidden = false")
    Integer getReviewCountForAstrologer(@Param("astrologerId") Long astrologerId);

    List<Review> findByIsApprovedFalseOrderByCreatedAtAsc(); // For CMS moderation

    List<Review> findByRatingAndIsApprovedAndIsHiddenOrderByHelpfulCountDesc(Integer rating, Boolean isApproved, Boolean isHidden);
}
