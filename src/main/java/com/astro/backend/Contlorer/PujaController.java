package com.astro.backend.Contlorer;



import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.RequestDTO.PujaBookingRequest;
import com.astro.backend.Services.PujaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/puja")
@RequiredArgsConstructor
public class PujaController {

    private final PujaRepository pujaRepo;
    private final PujaSlotRepository slotRepo;
    private final PujaService pujaService;
    private final PujaBookingRepository bookingRepo;

    @GetMapping("/list")
    public Object listPujas() {
        return pujaRepo.findAll();
    }

    @GetMapping("/slots/{pujaId}")
    public Object listSlots(@PathVariable Long pujaId) {
        return slotRepo.findByPujaIdAndStatus(pujaId, PujaSlot.SlotStatus.AVAILABLE);
    }

    @PostMapping("/book")
    public PujaBooking book(@RequestBody PujaBookingRequest req) {
        return pujaService.bookPuja(req.getUserId(), req.getPujaId(), req.getSlotId());
    }

    @GetMapping("/history/{userId}")
    public Object history(@PathVariable Long userId) {
        return bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
    }
}

