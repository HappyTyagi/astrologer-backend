package com.astro.backend.Repositry;

import com.astro.backend.Entity.BirthdayNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BirthdayNotificationRepository extends JpaRepository<BirthdayNotification, Long> {

    /**
     * Find pending birthday notifications not yet sent
     */
    List<BirthdayNotification> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find all notifications for a user
     */
    List<BirthdayNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find unviewed notifications for a user
     */
    List<BirthdayNotification> findByUserIdAndIsViewedFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Find notifications sent in a date range
     */
    @Query("SELECT bn FROM BirthdayNotification bn " +
           "WHERE bn.isSent = true " +
           "AND bn.emailSentAt >= :startDate " +
           "AND bn.emailSentAt <= :endDate")
    List<BirthdayNotification> findSentNotificationsInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Check if birthday notification already exists for user in current year
     */
    @Query("SELECT bn FROM BirthdayNotification bn " +
           "WHERE bn.userId = :userId " +
           "AND bn.upcomingYear = :year")
    Optional<BirthdayNotification> findByUserAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * Find all unviewed notifications across all users
     */
    List<BirthdayNotification> findByIsViewedFalseAndIsSentTrue();

    /**
     * Count sent notifications for a user
     */
    long countByUserIdAndIsSentTrue(Long userId);

    /**
     * Find expired birthday offers
     */
    @Query("SELECT bn FROM BirthdayNotification bn " +
           "WHERE bn.isSent = true " +
           "AND bn.offerValidTill < :now " +
           "AND bn.status != 'EXPIRED'")
    List<BirthdayNotification> findExpiredOffers(@Param("now") LocalDateTime now);
}
