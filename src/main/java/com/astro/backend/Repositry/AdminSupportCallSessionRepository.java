package com.astro.backend.Repositry;

import com.astro.backend.Entity.AdminSupportCallSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSupportCallSessionRepository extends JpaRepository<AdminSupportCallSession, String> {
    Optional<AdminSupportCallSession> findTopByChatIdOrderByCreatedAtDesc(String chatId);

    Optional<AdminSupportCallSession> findByIdAndChatId(String id, String chatId);

    Optional<AdminSupportCallSession> findTop1ByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, String status);
}
