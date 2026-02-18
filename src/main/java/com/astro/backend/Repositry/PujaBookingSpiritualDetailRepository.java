package com.astro.backend.Repositry;

import com.astro.backend.Entity.PujaBookingSpiritualDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PujaBookingSpiritualDetailRepository extends JpaRepository<PujaBookingSpiritualDetail, Long> {
    Optional<PujaBookingSpiritualDetail> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PujaBookingSpiritualDetail> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
