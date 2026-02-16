package com.astro.backend.Repositry;

import com.astro.backend.Entity.WebNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebNotificationRepository extends JpaRepository<WebNotification, Long> {
}

