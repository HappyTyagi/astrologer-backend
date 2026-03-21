package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Auth.JwtAuthFilter;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Helper.PujaOrderIdHelper;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.PujaBookingRequest;
import com.astro.backend.RequestDTO.PujaBookingPreferenceUpdateRequest;
import com.astro.backend.RequestDTO.PujaRescheduleRequest;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.ResponseDTO.AgoraTokenResponse;
import com.astro.backend.ResponseDTO.AstrologerDistrictPriceResponse;
import com.astro.backend.ResponseDTO.PujaRescheduleItemResponse;
import com.astro.backend.Services.AgoraTokenService;
import com.astro.backend.Services.AstrologerDistrictPriceService;
import com.astro.backend.Services.PujaService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Objects;
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
    private final UserRepository userRepository;
    private final AgoraTokenService agoraTokenService;

    @GetMapping("/list")
    public Object listPujas(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<Puja> pujas = pujaRepo.findAll();
        final String normalizedSearch = normalizeSearch(search);
        if (!normalizedSearch.isEmpty()) {
            pujas = pujas.stream()
                    .filter(puja ->
                            containsSearch(puja.getName(), normalizedSearch)
                                    || containsSearch(puja.getHiName(), normalizedSearch)
                                    || containsSearch(puja.getDescription(), normalizedSearch)
                                    || containsSearch(puja.getCategory(), normalizedSearch)
                                    || containsSearch(puja.getBenefits(), normalizedSearch)
                                    || containsSearch(puja.getRituals(), normalizedSearch))
                    .toList();
        }

        final boolean pagedRequested = page != null || size != null || !normalizedSearch.isEmpty();
        if (!pagedRequested) {
            return pujas;
        }

        final int pageIndex = normalizePage(page);
        final int pageSize = normalizeSize(size);
        final int total = pujas.size();
        final int fromIndex = Math.min(pageIndex * pageSize, total);
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<Puja> pageItems = pujas.subList(fromIndex, toIndex);
        final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", true);
        payload.put("message", "Pujas fetched successfully");
        payload.put("items", pageItems);
        payload.put("count", total);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("totalPages", totalPages);
        payload.put("hasNext", toIndex < total);
        payload.put("hasPrevious", pageIndex > 0);
        payload.put("search", normalizedSearch);
        return payload;
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
                req.getCustomGotraName(),
                req.getPaymentMethod(),
                req.getTransactionId(),
                req.getUseWallet(),
                req.getPackageCode(),
                req.getPackageName(),
                req.getPackagePrice(),
                req.getPackageDurationMinutes(),
                req.getRashiMasterId(),
                req.getNakshatraMasterId()
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

    @PostMapping("/booking-preferences/{userId}")
    public Object updateBookingPreferences(
            @PathVariable Long userId,
            @RequestBody PujaBookingPreferenceUpdateRequest request
    ) {
        return pujaService.updateUserBookingPreferences(
                userId,
                request.getGotraMasterId(),
                request.getCustomGotraName(),
                request.getRashiMasterId(),
                request.getNakshatraMasterId()
        );
    }

    @PostMapping("/slot-master/generate")
    public Object generateSlotsByMaster(@RequestBody PujaSlotMasterRequest request) {
        return Map.of(
                "status", false,
                "message", "Slot generation is admin controlled. Please create slots from admin panel."
        );
    }

    @GetMapping("/history/{userId}")
    public Object history(
            @PathVariable Long userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        final List<PujaBooking> bookings = bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
        final Set<Long> slotIds = bookings.stream()
                .map(PujaBooking::getSlotId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        final Set<Long> pujaIds = bookings.stream()
                .map(PujaBooking::getPujaId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        final Map<Long, PujaSlot> slotById = slotRepo.findAllById(slotIds)
                .stream()
                .collect(Collectors.toMap(PujaSlot::getId, s -> s));
        final Map<Long, Puja> pujaById = pujaRepo.findAllById(pujaIds)
                .stream()
                .collect(Collectors.toMap(Puja::getId, p -> p));

        final List<Map<String, Object>> rows = bookings.stream().map(b -> {
            PujaSlot slot = b.getSlotId() == null ? null : slotById.get(b.getSlotId());
            Puja puja = b.getPujaId() == null ? null : pujaById.get(b.getPujaId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("orderId", PujaOrderIdHelper.build(b.getUserId(), b.getId()));
            row.put("userId", b.getUserId());
            row.put("pujaId", b.getPujaId());
            row.put("pujaName", puja == null ? null : puja.getName());
            row.put("slotId", b.getSlotId());
            row.put("slotTime", slot == null ? null : slot.getSlotTime());
            row.put("slotSelectedByMobile", b.getSlotSelectedByMobile());
            row.put("addressId", b.getAddressId());
            row.put("bookedAt", b.getBookedAt());
            row.put("status", b.getStatus());
            row.put("totalPrice", b.getTotalPrice());
            row.put("paymentMethod", b.getPaymentMethod());
            row.put("transactionId", b.getTransactionId());
            row.put("pujaOtp", pujaService.ensurePujaOtp(b));
            row.put("startedAt", b.getStartedAt());
            row.put("completedAt", b.getCompletedAt());
            row.put("meetingLink", pujaService.resolveUserMeetingLink(b, slot));
            row.put("notificationStatus", b.getNotificationStatus());
            row.put("reminderSentAt", b.getReminderSentAt());
            return row;
        }).toList();

        final String normalizedSearch = normalizeSearch(search);
        final List<Map<String, Object>> filteredRows = rows.stream()
                .filter(row -> matchesHistorySearch(row, normalizedSearch))
                .toList();

        final boolean pagedRequested = page != null || size != null || !normalizedSearch.isEmpty();
        if (!pagedRequested) {
            return filteredRows;
        }

        final int pageIndex = normalizePage(page);
        final int pageSize = normalizeSize(size);
        final int total = filteredRows.size();
        final int fromIndex = Math.min(pageIndex * pageSize, total);
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<Map<String, Object>> pageItems = filteredRows.subList(fromIndex, toIndex);
        final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", true);
        payload.put("items", pageItems);
        payload.put("count", total);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("totalPages", totalPages);
        payload.put("hasNext", toIndex < total);
        payload.put("hasPrevious", pageIndex > 0);
        payload.put("search", normalizedSearch);
        return payload;
    }

    @PostMapping("/{bookingId}/generate-agora-link")
    public ResponseEntity<AgoraTokenResponse> generateAgoraLink(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User actor = requireCurrentUser(request);
            if (actor.getRole() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        AgoraTokenResponse.builder()
                                .success(false)
                                .message("Access denied")
                                .appId("")
                                .token("")
                                .channelName("")
                                .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                .tokenRequired(true)
                                .build()
                );
            }
            PujaBooking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
            Puja puja = booking.getPujaId() == null
                    ? null
                    : pujaRepo.findById(booking.getPujaId()).orElse(null);
            PujaSlot slot = booking.getSlotId() == null
                    ? null
                    : slotRepo.findById(booking.getSlotId()).orElse(null);

            if (booking.getStatus() == PujaBooking.BookingStatus.CANCELLED
                    || booking.getStatus() == PujaBooking.BookingStatus.REFUNDED
                    || booking.getStatus() == PujaBooking.BookingStatus.COMPLETED) {
                return ResponseEntity.badRequest().body(
                        AgoraTokenResponse.builder()
                                .success(false)
                                .message("Puja booking is not active.")
                                .appId("")
                                .token("")
                                .channelName("")
                                .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                .tokenRequired(true)
                                .build()
                );
            }

            if (actor.getRole() == Role.USER) {
                if (!Objects.equals(actor.getId(), booking.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            AgoraTokenResponse.builder()
                                    .success(false)
                                    .message("Access denied")
                                    .appId("")
                                    .token("")
                                    .channelName("")
                                    .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                    .tokenRequired(true)
                                    .build()
                    );
                }
            } else if (isPujaPerformer(actor)) {
                if (!isAssignedPujaPerformer(actor, puja)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            AgoraTokenResponse.builder()
                                    .success(false)
                                    .message("Access denied")
                                    .appId("")
                                    .token("")
                                    .channelName("")
                                    .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                    .tokenRequired(true)
                                    .build()
                    );
                }
                if (booking.getStartedAt() == null) {
                    return ResponseEntity.badRequest().body(
                            AgoraTokenResponse.builder()
                                    .success(false)
                                    .message("Please start puja with OTP first.")
                                    .appId("")
                                    .token("")
                                    .channelName("")
                                    .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                    .tokenRequired(true)
                                    .build()
                    );
                }
            } else if (actor.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        AgoraTokenResponse.builder()
                                .success(false)
                                .message("Access denied")
                                .appId("")
                                .token("")
                                .channelName("")
                                .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                .tokenRequired(true)
                                .build()
                );
            }

            if (slot == null || slot.getSlotTime() == null) {
                return ResponseEntity.badRequest().body(
                        AgoraTokenResponse.builder()
                                .success(false)
                                .message("Puja slot is not assigned yet.")
                                .appId("")
                                .token("")
                                .channelName("")
                                .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                .tokenRequired(true)
                                .build()
                );
            }

            LocalDateTime slotTime = slot.getSlotTime();
            LocalDateTime joinOpensAt = slotTime.minusMinutes(10);
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(joinOpensAt)) {
                return ResponseEntity.badRequest().body(
                        AgoraTokenResponse.builder()
                                .success(false)
                                .message("Join will be enabled 10 minutes before slot time.")
                                .appId("")
                                .token("")
                                .channelName("")
                                .uid(actor.getId() == null ? 0 : actor.getId().intValue())
                                .tokenRequired(true)
                                .build()
                );
            }

            String channel = booking.getAgoraChannel() == null ? "" : booking.getAgoraChannel().trim();
            if (channel.isEmpty()) {
                channel = "puja_" + booking.getId();
                booking.setAgoraChannel(channel);
                bookingRepo.save(booking);
            }

            int uid = actor.getId() == null ? 0 : actor.getId().intValue();
            AgoraTokenResponse response = agoraTokenService.generateRtcToken(channel, uid, "publisher");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgoraTokenResponse.builder()
                            .success(false)
                            .message("Failed to generate Agora link: " + e.getMessage())
                            .token("")
                            .appId("")
                            .channelName("")
                            .uid(0)
                            .tokenRequired(true)
                            .build());
        }
    }

    @PostMapping("/{bookingId}/start")
    public ResponseEntity<?> startPuja(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User actor = requireCurrentUser(request);
            if (!canManagePuja(actor)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "status", false,
                        "message", "Access denied"
                ));
            }

            PujaBooking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
            Puja puja = booking.getPujaId() == null
                    ? null
                    : pujaRepo.findById(booking.getPujaId()).orElse(null);
            PujaSlot slot = booking.getSlotId() == null
                    ? null
                    : slotRepo.findById(booking.getSlotId()).orElse(null);

            if (isPujaPerformer(actor)) {
                if (!isAssignedPujaPerformer(actor, puja)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "status", false,
                            "message", "Access denied"
                    ));
                }
            }

            if (booking.getStatus() == PujaBooking.BookingStatus.CANCELLED
                    || booking.getStatus() == PujaBooking.BookingStatus.REFUNDED) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Puja booking is not active"
                ));
            }
            if (booking.getStatus() == PujaBooking.BookingStatus.COMPLETED) {
                return ResponseEntity.ok(Map.of(
                        "status", true,
                        "message", "Puja already completed",
                        "bookingId", booking.getId(),
                        "startedAt", booking.getStartedAt(),
                        "completedAt", booking.getCompletedAt()
                ));
            }

            if (slot == null || slot.getSlotTime() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Puja slot is not assigned yet."
                ));
            }

            final LocalDateTime slotTime = slot.getSlotTime();
            final LocalDateTime joinOpensAt = slotTime.minusMinutes(10);
            final LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(joinOpensAt)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Puja can be started 10 minutes before slot time."
                ));
            }

            final String otp = readPujaOtp(payload);
            if (!otp.matches("^[0-9]{4}$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Invalid OTP. Please enter a 4-digit OTP."
                ));
            }

            final String expected = pujaService.ensurePujaOtp(booking);
            if (!expected.equals(otp)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Invalid OTP"
                ));
            }

            if (booking.getStartedAt() != null) {
                return ResponseEntity.ok(Map.of(
                        "status", true,
                        "message", "Puja already started",
                        "bookingId", booking.getId(),
                        "startedAt", booking.getStartedAt()
                ));
            }

            booking.setStartedAt(LocalDateTime.now());
            bookingRepo.save(booking);
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", "Puja started successfully",
                    "bookingId", booking.getId(),
                    "startedAt", booking.getStartedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", false,
                    "message", "Failed to start puja: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{bookingId}/end")
    public ResponseEntity<?> endPuja(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request
    ) {
        try {
            User actor = requireCurrentUser(request);
            if (!canManagePuja(actor)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "status", false,
                        "message", "Access denied"
                ));
            }

            PujaBooking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
            Puja puja = booking.getPujaId() == null
                    ? null
                    : pujaRepo.findById(booking.getPujaId()).orElse(null);

            if (isPujaPerformer(actor)) {
                if (!isAssignedPujaPerformer(actor, puja)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "status", false,
                            "message", "Access denied"
                    ));
                }
            }

            if (booking.getStatus() == PujaBooking.BookingStatus.CANCELLED
                    || booking.getStatus() == PujaBooking.BookingStatus.REFUNDED) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Puja booking is not active"
                ));
            }
            if (booking.getStatus() == PujaBooking.BookingStatus.COMPLETED) {
                return ResponseEntity.ok(Map.of(
                        "status", true,
                        "message", "Puja already completed",
                        "bookingId", booking.getId(),
                        "startedAt", booking.getStartedAt(),
                        "completedAt", booking.getCompletedAt()
                ));
            }

            if (booking.getStartedAt() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Puja is not started yet."
                ));
            }

            final String otp = readPujaOtp(payload);
            if (!otp.matches("^[0-9]{4}$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Invalid OTP. Please enter a 4-digit OTP."
                ));
            }

            final String expected = pujaService.ensurePujaOtp(booking);
            if (!expected.equals(otp)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Invalid OTP"
                ));
            }

            booking.setStatus(PujaBooking.BookingStatus.COMPLETED);
            booking.setCompletedAt(LocalDateTime.now());
            bookingRepo.save(booking);
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", "Puja completed successfully",
                    "bookingId", booking.getId(),
                    "startedAt", booking.getStartedAt(),
                    "completedAt", booking.getCompletedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", false,
                    "message", "Failed to end puja: " + e.getMessage()
            ));
        }
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

    private User requireCurrentUser(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthFilter.AUTH_USER_ID_ATTR);
        Long currentUserId = null;
        if (value instanceof Long longValue) {
            currentUserId = longValue;
        } else if (value instanceof Integer intValue) {
            currentUserId = intValue.longValue();
        } else if (value != null) {
            currentUserId = Long.parseLong(String.valueOf(value));
        }
        if (currentUserId == null || currentUserId <= 0) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    private String readPujaOtp(Map<String, Object> payload) {
        if (payload == null) {
            return "";
        }
        Object value = payload.get("otp");
        if (value == null) {
            value = payload.get("pujaOtp");
        }
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isPujaPerformer(User actor) {
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        return actor.getRole() == Role.ASTROLOGER || actor.getRole() == Role.PANDIT;
    }

    private boolean canManagePuja(User actor) {
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        return actor.getRole() == Role.ADMIN || isPujaPerformer(actor);
    }

    private boolean isAssignedPujaPerformer(User actor, Puja puja) {
        if (!isPujaPerformer(actor)) {
            return false;
        }
        if (puja == null || puja.getAstrologerId() == null) {
            return false;
        }
        return Objects.equals(puja.getAstrologerId(), actor.getId());
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

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim().toLowerCase();
    }

    private boolean containsSearch(String value, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase().contains(search);
    }

    private boolean matchesHistorySearch(Map<String, Object> row, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        return containsSearchValue(row.get("orderId"), search)
                || containsSearchValue(row.get("status"), search)
                || containsSearchValue(row.get("paymentMethod"), search)
                || containsSearchValue(row.get("transactionId"), search)
                || containsSearchValue(row.get("pujaName"), search)
                || containsSearchValue(row.get("pujaId"), search);
    }

    private boolean containsSearchValue(Object value, String search) {
        if (value == null) {
            return false;
        }
        return value.toString().toLowerCase().contains(search);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 12;
        }
        return Math.min(size, 100);
    }
}
