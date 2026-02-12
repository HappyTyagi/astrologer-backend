package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.WebAdminPujaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/web/puja")
@RequiredArgsConstructor
public class AdminPujaController {

    private final PujaRepository pujaRepository;
    private final PujaBookingRepository pujaBookingRepository;
    private final PujaSlotRepository pujaSlotRepository;
    private final UserRepository userRepository;

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
            rows.add(row);
        }

        return ResponseEntity.ok(Map.of(
                "status", true,
                "pujaId", pujaId,
                "totalRegistrations", rows.size(),
                "registrations", rows
        ));
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
