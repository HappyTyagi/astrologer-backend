package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.RequestDTO.PujaBookingRequest;
import com.astro.backend.RequestDTO.PujaRescheduleRequest;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.ResponseDTO.AstrologerDistrictPriceResponse;
import com.astro.backend.ResponseDTO.PujaRescheduleItemResponse;
import com.astro.backend.Services.AstrologerDistrictPriceService;
import com.astro.backend.Services.PujaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/puja")
@RequiredArgsConstructor
public class PujaController {

    private final PujaRepository pujaRepo;
    private final PujaSlotRepository slotRepo;
    private final PujaService pujaService;
    private final PujaBookingRepository bookingRepo;
    private final AstrologerDistrictPriceService astrologerDistrictPriceService;

    @GetMapping("/list")
    public Object listPujas() {
        return pujaRepo.findAll();
    }

    @GetMapping("/popup/daily")
    public Object dailyPopupPuja() {
        LocalDate today = LocalDate.now();
        List<Puja> activePujas = pujaRepo.findByIsActiveTrue()
                .stream()
                .filter(p -> p.getStatus() == null || !"INACTIVE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> Boolean.TRUE.equals(p.getPopupEnabled()))
                .filter(p -> p.getPopupStartDate() == null || !today.isBefore(p.getPopupStartDate()))
                .filter(p -> p.getPopupEndDate() == null || !today.isAfter(p.getPopupEndDate()))
                .sorted(
                        Comparator
                                .comparing((Puja p) -> p.getPopupPriority() == null ? 0 : p.getPopupPriority())
                                .reversed()
                                .thenComparing(Puja::getId)
                )
                .toList();

        if (activePujas.isEmpty()) {
            return java.util.Map.of(
                    "status", false,
                    "message", "No active puja available for popup"
            );
        }

        // Date-wise deterministic random selection so popup changes by day
        long seed = today.toEpochDay();
        int index = new Random(seed).nextInt(activePujas.size());
        Puja selected = activePujas.get(index);

        return java.util.Map.of(
                "status", true,
                "message", "Daily popup puja fetched successfully",
                "date", today.toString(),
                "puja", selected
        );
    }

    @GetMapping("/slots/{pujaId}")
    public Object listSlots(@PathVariable Long pujaId) {
        pujaService.ensureDefaultSlotsForPuja(pujaId);
        LocalDateTime now = LocalDateTime.now();
        return slotRepo.findByPujaIdOrderBySlotTimeAsc(pujaId)
                .stream()
                .filter(slot -> slot.getStatus() != PujaSlot.SlotStatus.CANCELLED)
                .filter(slot -> slot.getSlotTime() == null || slot.getSlotTime().isAfter(now))
                .filter(slot -> {
                    if (slot.getSlotTime() == null) return false;
                    final int hour = slot.getSlotTime().getHour();
                    return hour >= 8 && hour < 20;
                })
                .toList();
    }

    @PostMapping("/book")
    public PujaBooking book(@RequestBody PujaBookingRequest req) {
        return pujaService.bookPuja(
                req.getUserId(),
                req.getPujaId(),
                req.getSlotId(),
                req.getAddressId(),
                req.getPaymentMethod(),
                req.getTransactionId(),
                req.getUseWallet()
        );
    }

    @PostMapping("/slot-master/generate")
    public Object generateSlotsByMaster(@RequestBody PujaSlotMasterRequest request) {
        try {
            return pujaService.generateSlotsFromMaster(request);
        } catch (Exception e) {
            return Map.of(
                    "status", false,
                    "message", e.getMessage()
            );
        }
    }

    @GetMapping("/history/{userId}")
    public Object history(@PathVariable Long userId) {
        final List<PujaBooking> bookings = bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
        final Set<Long> slotIds = bookings.stream()
                .map(PujaBooking::getSlotId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        final Map<Long, LocalDateTime> slotTimeById = slotRepo.findAllById(slotIds)
                .stream()
                .collect(Collectors.toMap(PujaSlot::getId, PujaSlot::getSlotTime));

        return bookings.stream().map(b -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("userId", b.getUserId());
            row.put("pujaId", b.getPujaId());
            row.put("slotId", b.getSlotId());
            row.put("slotTime", b.getSlotId() == null ? null : slotTimeById.get(b.getSlotId()));
            row.put("addressId", b.getAddressId());
            row.put("bookedAt", b.getBookedAt());
            row.put("status", b.getStatus());
            row.put("totalPrice", b.getTotalPrice());
            row.put("paymentMethod", b.getPaymentMethod());
            row.put("transactionId", b.getTransactionId());
            row.put("meetingLink", b.getMeetingLink());
            row.put("notificationStatus", b.getNotificationStatus());
            row.put("reminderSentAt", b.getReminderSentAt());
            return row;
        }).toList();
    }

    @PostMapping("/history/{orderId}/resend-receipt")
    public Object resendReceipt(
            @PathVariable String orderId,
            @RequestBody ResendReceiptRequest request
    ) {
        try {
            return pujaService.resendReceiptEmail(orderId, request);
        } catch (Exception e) {
            return Map.of(
                    "status", false,
                    "message", e.getMessage()
            );
        }
    }

    @GetMapping("/reschedule-list/{userId}")
    public List<PujaRescheduleItemResponse> getRescheduleList(@PathVariable Long userId) {
        return pujaService.getUserRescheduleBookings(userId);
    }

    @PostMapping("/reschedule")
    public Object reschedule(@RequestBody PujaRescheduleRequest request) {
        try {
            PujaBooking updated = pujaService.reschedulePuja(
                    request.getUserId(),
                    request.getBookingId(),
                    request.getNewSlotId()
            );
            return Map.of(
                    "status", true,
                    "message", "Puja rescheduled successfully",
                    "booking", updated
            );
        } catch (Exception e) {
            return Map.of(
                    "status", false,
                    "message", e.getMessage()
            );
        }
    }

    @GetMapping("/location-price/{pujaId}/{districtMasterId}")
    public AstrologerDistrictPriceResponse getLocationPrice(
            @PathVariable Long pujaId,
            @PathVariable Long districtMasterId,
            @RequestParam(required = false) Long astrologerId) {
        try {
            return astrologerDistrictPriceService.getLocationBasedPrice(pujaId, districtMasterId, astrologerId);
        } catch (Exception e) {
            return AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build();
        }
    }
}
