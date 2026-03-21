package com.astro.backend.Repositry;

import com.astro.backend.Entity.ChatNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatNotificationRepository extends JpaRepository<ChatNotification, Long> {
    List<ChatNotification> findTop200ByUserIdAndTypeAndIsReadFalseAndIsActiveTrueOrderByCreatedAtAsc(
            Long userId,
            ChatNotification.NotificationType type
    );

    Optional<ChatNotification> findByIdAndUserId(Long id, Long userId);
}
