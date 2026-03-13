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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        Puja puja = pujaRepo.findById(pujaId).orElse(null);
        if (puja == null) {
            return List.of();
        }
        boolean slotBookingEnabled = puja.getIsSlot() != null
                ? Boolean.TRUE.equals(puja.getIsSlot())
                : pujaService.isMobileSlotSelectionEnabled();
        if (!slotBookingEnabled) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return slotRepo.findByPujaIdOrderBySlotTimeAsc(pujaId)
                .stream()
                .filter(slot -> !Boolean.FALSE.equals(slot.getIsActive()))
                .filter(slot -> slot.getStatus() != PujaSlot.SlotStatus.CANCELLED)
                .filter(slot -> slot.getSlotTime() == null || slot.getSlotTime().isAfter(now))
                .filter(slot -> {
                    if (slot.getSlotTime() == null) return false;
                    final int hour = slot.getSlotTime().getHour();
                    return hour >= 8 && hour < 20;
                })
                .toList();
    }

    @GetMapping("/samagri/{pujaId}")
    public Object pujaSamagri(@PathVariable Long pujaId) {
        return Map.of(
                "status", true,
                "pujaId", pujaId,
                "items", pujaService.getPujaSamagriForMobile(pujaId)
        );
    }

    @PostMapping("/book")
    public PujaBooking book(@RequestBody PujaBookingRequest req) {
        return pujaService.bookPuja(
                req.getUserId(),
                req.getPujaId(),
                req.getSlotId(),
                req.getAddressId(),
                req.getGotraMasterId(),
                req.getPaymentMethod(),
                req.getTransactionId(),
                req.getUseWallet()
        );
    }

    @GetMapping("/booking-masters")
    public Object bookingMasters() {
        return pujaService.getBookingMasters();
    }

    @GetMapping("/booking-preferences/{userId}")
    public Object bookingPreferences(@PathVariable Long userId) {
        return pujaService.getUserBookingPreferences(userId);
    }

    @PostMapping("/slot-master/generate")
    public Object generateSlotsByMaster(@RequestBody PujaSlotMasterRequest request) {
        return Map.of(
                "status", false,
                "message", "Slot generation is admin controlled. Please create slots from admin panel."
        );
    }

    @GetMapping("/history/{userId}")
    public Object history(@PathVariable Long userId) {
        final List<PujaBooking> bookings = bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
        final Set<Long> slotIds = bookings.stream()
                .map(PujaBooking::getSlotId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        final Map<Long, PujaSlot> slotById = slotRepo.findAllById(slotIds)
                .stream()
                .collect(Collectors.toMap(PujaSlot::getId, s -> s));

        return bookings.stream().map(b -> {
            PujaSlot slot = b.getSlotId() == null ? null : slotById.get(b.getSlotId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("userId", b.getUserId());
            row.put("pujaId", b.getPujaId());
            row.put("slotId", b.getSlotId());
            row.put("slotTime", slot == null ? null : slot.getSlotTime());
            row.put("slotSelectedByMobile", b.getSlotSelectedByMobile());
            row.put("addressId", b.getAddressId());
            row.put("bookedAt", b.getBookedAt());
            row.put("status", b.getStatus());
            row.put("totalPrice", b.getTotalPrice());
            row.put("paymentMethod", b.getPaymentMethod());
            row.put("transactionId", b.getTransactionId());
            row.put("meetingLink", pujaService.resolveUserMeetingLink(b, slot));
            row.put("notificationStatus", b.getNotificationStatus());
            row.put("reminderSentAt", b.getReminderSentAt());
            return row;
        }).toList();
    }

    @GetMapping("/join-access/{orderId}")
    public Object joinAccess(
            @PathVariable String orderId,
            @RequestParam String token,
            @RequestParam(required = false) String date
    ) {
        return pujaService.getJoinAccess(orderId, token, date);
    }

    @GetMapping(value = "/join/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> joinPuja(
            @PathVariable String orderId,
            @RequestParam String token,
            @RequestParam(required = false) String date
    ) {
        Map<String, Object> access = pujaService.getJoinAccess(orderId, token, date);
        if (Boolean.TRUE.equals(access.get("allowed"))) {
            String meetingLink = access.get("meetingLink") == null
                    ? ""
                    : access.get("meetingLink").toString().trim();
            if (!meetingLink.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(java.net.URI.create(meetingLink));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildJoinBlockedHtml(access));
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

    private String buildJoinBlockedHtml(Map<String, Object> access) {
        String message = access == null || access.get("message") == null
                ? "Join link is not available right now."
                : access.get("message").toString();
        String orderId = access == null || access.get("orderId") == null
                ? "-"
                : access.get("orderId").toString();
        String slotTime = access == null || access.get("slotTime") == null
                ? "-"
                : access.get("slotTime").toString();
        String joinOpensAt = access == null || access.get("joinOpensAt") == null
                ? "-"
                : access.get("joinOpensAt").toString();

        return """
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Puja Join Status</title>
                </head>
                <body style="margin:0;padding:0;background:#f6f8fc;font-family:Arial,sans-serif;color:#1f2533;">
                  <div style="max-width:720px;margin:36px auto;padding:0 14px;">
                    <div style="background:#fff;border:1px solid #e4e9f2;border-radius:14px;padding:22px;">
                      <h2 style="margin:0 0 10px 0;color:#1f2f73;">Puja Join Status</h2>
                      <p style="margin:0 0 16px 0;color:#5c6577;">%s</p>
                      <table style="width:100%%;border-collapse:collapse;">
                        <tr><td style="padding:8px 0;color:#607086;">Order</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#607086;">Slot Time</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#607086;">Join Opens At</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                      </table>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(message),
                escapeHtml(orderId),
                escapeHtml(slotTime),
                escapeHtml(joinOpensAt)
        );
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
