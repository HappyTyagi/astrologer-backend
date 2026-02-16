package com.astro.backend.Repositry;

import com.astro.backend.Entity.ChatNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatNotificationRepository extends JpaRepository<ChatNotification, Long> {
}

