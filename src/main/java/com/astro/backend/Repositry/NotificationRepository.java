package com.astro.backend.Repositry;

import com.astro.backend.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :fromDate ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    Integer countByUserIdAndIsRead(Long userId, Boolean isRead);

    void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime dateTime);

    List<Notification> findByDeliveryStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}
