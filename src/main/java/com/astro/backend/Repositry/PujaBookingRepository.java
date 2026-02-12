package com.astro.backend.Repositry;


import com.astro.backend.Entity.PujaBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PujaBookingRepository extends JpaRepository<PujaBooking, Long> {
    List<PujaBooking> findByUserIdOrderByBookedAtDesc(Long userId);
    java.util.Optional<PujaBooking> findByIdAndUserId(Long id, Long userId);
    List<PujaBooking> findByPujaIdOrderByBookedAtDesc(Long pujaId);
    long countByStatusIn(java.util.Collection<PujaBooking.BookingStatus> statuses);
}
