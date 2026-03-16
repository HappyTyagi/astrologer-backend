package com.astro.backend.Repositry;

import com.astro.backend.Entity.AdminSupportChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminSupportChatSessionRepository extends JpaRepository<AdminSupportChatSession, String> {
    Optional<AdminSupportChatSession> findByUserIdAndAdminUserId(Long userId, Long adminUserId);

    List<AdminSupportChatSession> findAllByOrderByLastMessageAtDesc();

    List<AdminSupportChatSession> findByUserIdOrderByLastMessageAtDesc(Long userId);
}
