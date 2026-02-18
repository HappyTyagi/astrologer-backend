package com.astro.backend.Repositry;


import com.astro.backend.Entity.PujaBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.time.LocalDateTime;

public interface PujaBookingRepository extends JpaRepository<PujaBooking, Long> {
    List<PujaBooking> findByUserIdOrderByBookedAtDesc(Long userId);
    java.util.Optional<PujaBooking> findByIdAndUserId(Long id, Long userId);
    List<PujaBooking> findByPujaIdOrderByBookedAtDesc(Long pujaId);
    long countByStatusIn(java.util.Collection<PujaBooking.BookingStatus> statuses);
    List<PujaBooking> findByStatusIn(Collection<PujaBooking.BookingStatus> statuses);
    List<PujaBooking> findByStatusInAndReminderSentAtIsNull(Collection<PujaBooking.BookingStatus> statuses);
    List<PujaBooking> findByBookedAtBetweenOrderByBookedAtDesc(LocalDateTime from, LocalDateTime to);
}
