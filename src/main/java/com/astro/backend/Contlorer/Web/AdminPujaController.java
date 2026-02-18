package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaBookingSpiritualDetail;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaBookingSpiritualDetailRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.PujaBookingSpiritualUpdateRequest;
import com.astro.backend.RequestDTO.AdminBookingSlotAssignRequest;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.RequestDTO.WebAdminPujaRequest;
import com.astro.backend.Services.PujaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/web/puja")
@RequiredArgsConstructor
public class AdminPujaController {

    private final PujaRepository pujaRepository;
    private final PujaBookingRepository pujaBookingRepository;
    private final PujaBookingSpiritualDetailRepository pujaBookingSpiritualDetailRepository;
    private final PujaSlotRepository pujaSlotRepository;
    private final UserRepository userRepository;
    private final PujaService pujaService;

    @GetMapping
    public ResponseEntity<?> listWithPastSegregation() {
        syncPastPujas();
        List<Puja> all = pujaRepository.findAll()
                .stream()
                .filter(p -> !Boolean.FALSE.equals(p.getIsActive()))
                .sorted(Comparator.comparing(Puja::getId))
                .toList();

        List<Puja> active = all.stream()
                .filter(p -> !"PAST".equalsIgnoreCase(defaultText(p.getStatus())))
                .toList();
        List<Puja> past = all.stream()
                .filter(p -> "PAST".equalsIgnoreCase(defaultText(p.getStatus())))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("activePuja", active);
        response.put("pastPuja", past);
        response.put("countActive", active.size());
        response.put("countPast", past.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody WebAdminPujaRequest request) {
        validateDates(request);
        Puja saved = pujaRepository.save(buildOrUpdate(new Puja(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody WebAdminPujaRequest request) {
        validateDates(request);
        Puja existing = pujaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Puja not found: " + id));
        Puja updated = buildOrUpdate(existing, request);
        return ResponseEntity.ok(pujaRepository.save(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Puja existing = pujaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Puja not found: " + id));
        existing.setIsActive(false);
        existing.setStatus("INACTIVE");
        pujaRepository.save(existing);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Puja deleted successfully",
                "id", id
        ));
    }

    @GetMapping("/{pujaId}/registrations")
    public ResponseEntity<?> getPujaRegistrations(@PathVariable Long pujaId) {
        List<PujaBooking> bookings = pujaBookingRepository.findByPujaIdOrderByBookedAtDesc(pujaId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PujaBooking booking : bookings) {
            Optional<User> userOpt = userRepository.findById(booking.getUserId());
            Optional<PujaSlot> slotOpt = pujaSlotRepository.findById(booking.getSlotId());
            PujaBookingSpiritualDetail spiritual = pujaBookingSpiritualDetailRepository
                    .findTopByBookingIdOrderByCreatedAtDesc(booking.getId())
                    .orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId", booking.getId());
            row.put("userId", booking.getUserId());
            row.put("userName", userOpt.map(User::getName).orElse("Unknown"));
            row.put("mobileNumber", userOpt.map(User::getMobileNumber).orElse(""));
            row.put("email", userOpt.map(User::getEmail).orElse(""));
            row.put("slotId", booking.getSlotId());
            row.put("slotTime", slotOpt.map(PujaSlot::getSlotTime).orElse(null));
            row.put("bookingStatus", booking.getStatus());
            row.put("bookedAt", booking.getBookedAt());
            row.put("addressId", booking.getAddressId());
            row.put("gotraMasterId", spiritual == null ? null : spiritual.getGotraMasterId());
            row.put("gotraName", spiritual == null ? null : spiritual.getGotraName());
            row.put("rashiMasterId", spiritual == null ? null : spiritual.getRashiMasterId());
            row.put("rashiName", spiritual == null ? null : spiritual.getRashiName());
            row.put("nakshatraMasterId", spiritual == null ? null : spiritual.getNakshatraMasterId());
            row.put("nakshatraName", spiritual == null ? null : spiritual.getNakshatraName());
            rows.add(row);
        }

        return ResponseEntity.ok(Map.of(
                "status", true,
                "pujaId", pujaId,
                "totalRegistrations", rows.size(),
                "registrations", rows
        ));
    }

    @GetMapping("/{pujaId}/slots")
    public ResponseEntity<?> getPujaSlots(@PathVariable Long pujaId) {
        List<PujaSlot> slots = pujaSlotRepository.findByPujaIdOrderBySlotTimeAsc(pujaId);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "pujaId", pujaId,
                "count", slots.size(),
                "slots", slots
        ));
    }

    @PostMapping("/slot-master/generate")
    public ResponseEntity<?> generateSlotsByAdmin(@RequestBody PujaSlotMasterRequest request) {
        try {
            return ResponseEntity.ok(pujaService.generateSlotsFromMaster(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{pujaId}/slots")
    public ResponseEntity<?> createSlot(@PathVariable Long pujaId, @RequestBody Map<String, Object> payload) {
        Puja puja = pujaRepository.findById(pujaId)
                .orElseThrow(() -> new RuntimeException("Puja not found: " + pujaId));

        String slotTimeRaw = payload.get("slotTime") == null ? "" : payload.get("slotTime").toString().trim();
        if (slotTimeRaw.isEmpty()) {
            throw new RuntimeException("slotTime is required in ISO format, e.g. 2026-02-20T14:00:00");
        }
        LocalDateTime slotTime;
        try {
            slotTime = LocalDateTime.parse(slotTimeRaw);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("Invalid slotTime format. Use ISO date-time, e.g. 2026-02-20T14:00:00");
        }

        if (pujaSlotRepository.existsByPujaIdAndSlotTime(pujaId, slotTime)) {
            throw new RuntimeException("Slot already exists for this puja and time.");
        }

        Integer maxBookings = payload.get("maxBookings") instanceof Number
                ? ((Number) payload.get("maxBookings")).intValue()
                : 1;
        Long astrologerId = payload.get("astrologerId") instanceof Number
                ? ((Number) payload.get("astrologerId")).longValue()
                : null;

        PujaSlot slot = PujaSlot.builder()
                .pujaId(pujaId)
                .slotTime(slotTime)
                .status(PujaSlot.SlotStatus.AVAILABLE)
                .astrologerId(astrologerId)
                .maxBookings(maxBookings <= 0 ? 1 : maxBookings)
                .currentBookings(0)
                .isRecurring(false)
                .recurringPattern(null)
                .isActive(true)
                .build();

        PujaSlot saved = pujaSlotRepository.save(slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", true,
                "message", "Puja slot created by admin",
                "pujaName", puja.getName(),
                "slot", saved
        ));
    }

    @PutMapping("/slots/{slotId}/status")
    public ResponseEntity<?> updateSlotStatus(
            @PathVariable Long slotId,
            @RequestParam String status,
            @RequestParam(required = false) Boolean isActive
    ) {
        PujaSlot slot = pujaSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));

        PujaSlot.SlotStatus resolvedStatus;
        try {
            resolvedStatus = PujaSlot.SlotStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid status. Allowed: AVAILABLE, BOOKED, EXPIRED, CANCELLED");
        }

        slot.setStatus(resolvedStatus);
        if (isActive != null) {
            slot.setIsActive(isActive);
        } else if (resolvedStatus == PujaSlot.SlotStatus.CANCELLED) {
            slot.setIsActive(false);
        }
        PujaSlot saved = pujaSlotRepository.save(slot);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Slot updated by admin",
                "slot", saved
        ));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<?> removeSlot(@PathVariable Long slotId) {
        PujaSlot slot = pujaSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));

        slot.setStatus(PujaSlot.SlotStatus.CANCELLED);
        slot.setIsActive(false);
        pujaSlotRepository.save(slot);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Slot removed by admin",
                "slotId", slotId
        ));
    }

    @GetMapping("/bookings/today/segregated")
    public ResponseEntity<?> getTodayBookingsSegregated() {
        return ResponseEntity.ok(pujaService.getTodayBookingSegregationForAdmin());
    }

    @PutMapping("/bookings/{bookingId}/spiritual-details")
    public ResponseEntity<?> updateBookingSpiritualDetails(
            @PathVariable Long bookingId,
            @RequestBody PujaBookingSpiritualUpdateRequest request
    ) {
        return ResponseEntity.ok(
                pujaService.updateBookingSpiritualDetails(
                        bookingId,
                        request == null ? null : request.getGotraMasterId(),
                        request == null ? null : request.getRashiMasterId(),
                        request == null ? null : request.getNakshatraMasterId()
                )
        );
    }

    @PutMapping("/bookings/{bookingId}/slot")
    public ResponseEntity<?> assignBookingSlotByAdmin(
            @PathVariable Long bookingId,
            @RequestBody AdminBookingSlotAssignRequest request
    ) {
        return ResponseEntity.ok(
                pujaService.assignBookingSlotByAdmin(
                        bookingId,
                        request == null ? null : request.getSlotId()
                )
        );
    }

    @PostMapping("/bookings/{bookingId}/finalize")
    public ResponseEntity<?> finalizeBookingByAdmin(@PathVariable Long bookingId) {
        return ResponseEntity.ok(
                pujaService.finalizeBookingByAdmin(bookingId)
        );
    }

    private void syncPastPujas() {
        LocalDate today = LocalDate.now();
        List<Puja> candidates = pujaRepository.findByPopupEndDateBeforeAndIsActiveTrue(today);
        for (Puja puja : candidates) {
            if (!"PAST".equalsIgnoreCase(defaultText(puja.getStatus()))) {
                puja.setStatus("PAST");
                pujaRepository.save(puja);
            }
        }
    }

    private void validateDates(WebAdminPujaRequest request) {
        if (request.getPopupEndDate().isBefore(request.getPopupStartDate())) {
            throw new RuntimeException("popupEndDate must be greater than or equal to popupStartDate");
        }
    }

    private Puja buildOrUpdate(Puja puja, WebAdminPujaRequest request) {
        puja.setName(request.getName().trim());
        puja.setDescription(request.getDescription().trim());
        puja.setPrice(request.getPrice());
        puja.setDurationMinutes(request.getDurationMinutes());
        puja.setCategory(request.getCategory().trim());
        puja.setBenefits(request.getBenefits().trim());
        puja.setRituals(request.getRituals().trim());
        puja.setImage(request.getImage());
        puja.setPopupEnabled(request.getPopupEnabled());
        puja.setPopupStartDate(request.getPopupStartDate());
        puja.setPopupEndDate(request.getPopupEndDate());
        puja.setPopupPriority(request.getPopupPriority() == null ? 0 : request.getPopupPriority());
        puja.setPopupLabel(defaultText(request.getPopupLabel()).isBlank() ? "Upcoming" : request.getPopupLabel().trim());
        puja.setStatus(defaultText(request.getStatus()).isBlank() ? "ACTIVE" : request.getStatus().trim().toUpperCase());
        puja.setAstrologerId(request.getAstrologerId());
        puja.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : Boolean.TRUE);
        puja.setIsActive(true);
        return puja;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
