package com.astro.backend.Services;


import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PujaService {

    private final PujaRepository pujaRepo;
    private final PujaSlotRepository slotRepo;
    private final PujaBookingRepository bookingRepo;
    private final WalletService walletService;

    public PujaBooking bookPuja(Long userId, Long pujaId, Long slotId) {

        Puja puja = pujaRepo.findById(pujaId)
                .orElseThrow(() -> new RuntimeException("Invalid Puja"));

        PujaSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Invalid Slot"));

        if (slot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
            throw new RuntimeException("Slot not available");
        }

        boolean debited = walletService.debit(
                userId,
                puja.getPrice(),
                "PUJA_BOOKING",
                "Puja booking: " + puja.getName()
        );

        if (!debited) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        // Update slot
        slot.setStatus(PujaSlot.SlotStatus.BOOKED);
        slotRepo.save(slot);

        // Create booking
        PujaBooking booking = PujaBooking.builder()
                .userId(userId)
                .pujaId(pujaId)
                .slotId(slotId)
                .bookedAt(LocalDateTime.now())
                .status(PujaBooking.BookingStatus.CONFIRMED)
                .build();

        return bookingRepo.save(booking);
    }
}
