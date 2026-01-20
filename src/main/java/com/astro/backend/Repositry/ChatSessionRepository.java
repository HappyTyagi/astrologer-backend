package com.astro.backend.Repositry;
import com.astro.backend.Entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByUserIdAndStatus(Long userId, ChatSession.Status status);
}
