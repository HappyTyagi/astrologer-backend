package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Auth.JwtAuthFilter;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.Repositry.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pandit")
@RequiredArgsConstructor
public class PanditPujaController {

    private final PujaBookingRepository pujaBookingRepository;
    private final PujaSlotRepository pujaSlotRepository;
    private final PujaRepository pujaRepository;
    private final UserRepository userRepository;

    @GetMapping("/upcoming-pujas")
    public ResponseEntity<?> upcomingPujas(HttpServletRequest request) {
        User actor = requireCurrentUser(request);
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.ASTROLOGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", false,
                    "message", "Access denied"
            ));
        }

        final List<PujaBooking> all = pujaBookingRepository.findByStatusIn(
                List.of(PujaBooking.BookingStatus.CONFIRMED, PujaBooking.BookingStatus.PENDING)
        );
        if (all.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "count", 0,
                    "bookings", List.of()
            ));
        }

        final Set<Long> userIds = all.stream()
                .map(PujaBooking::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Set<Long> pujaIds = all.stream()
                .map(PujaBooking::getPujaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Set<Long> slotIds = all.stream()
                .map(PujaBooking::getSlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        final Map<Long, Puja> pujaMap = pujaRepository.findAllById(pujaIds)
                .stream()
                .collect(Collectors.toMap(Puja::getId, p -> p, (a, b) -> a));
        final Map<Long, PujaSlot> slotMap = pujaSlotRepository.findAllById(slotIds)
                .stream()
                .collect(Collectors.toMap(PujaSlot::getId, s -> s, (a, b) -> a));

        final LocalDateTime now = LocalDateTime.now();
        final List<PujaBooking> filtered = all.stream()
                .filter(booking -> {
                    Puja puja = booking.getPujaId() == null ? null : pujaMap.get(booking.getPujaId());
                    if (actor.getRole() == Role.ASTROLOGER) {
                        if (puja == null || puja.getAstrologerId() == null) return false;
                        return Objects.equals(puja.getAstrologerId(), actor.getId());
                    }
                    return true;
                })
                .filter(booking -> {
                    if (booking.getSlotId() == null) return true; // slot pending
                    PujaSlot slot = slotMap.get(booking.getSlotId());
                    if (slot == null || slot.getSlotTime() == null) return true;
                    return slot.getSlotTime().isAfter(now);
                })
                .sorted(Comparator.comparing(
                        (PujaBooking booking) -> {
                            if (booking.getSlotId() == null) return null;
                            PujaSlot slot = slotMap.get(booking.getSlotId());
                            return slot == null ? null : slot.getSlotTime();
                        },
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(
                        PujaBooking::getBookedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        final List<Map<String, Object>> rows = filtered.stream()
                .map(booking -> {
                    User user = booking.getUserId() == null ? null : userMap.get(booking.getUserId());
                    Puja puja = booking.getPujaId() == null ? null : pujaMap.get(booking.getPujaId());
                    PujaSlot slot = booking.getSlotId() == null ? null : slotMap.get(booking.getSlotId());

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("bookingId", booking.getId());
                    row.put("userId", booking.getUserId());
                    row.put("userName", user == null ? "Unknown" : defaultText(user.getName()));
                    if (actor.getRole() != Role.ASTROLOGER) {
                        row.put("mobileNumber", user == null ? "" : defaultText(user.getMobileNumber()));
                        row.put("email", user == null ? "" : defaultText(user.getEmail()));
                    }
                    row.put("pujaId", booking.getPujaId());
                    row.put("pujaName", puja == null ? "" : defaultText(puja.getName()));
                    row.put("pujaImage", puja == null ? null : puja.getImage());
                    row.put("slotId", booking.getSlotId());
                    row.put("slotTime", slot == null ? null : slot.getSlotTime());
                    row.put("bookingStatus", booking.getStatus() == null ? null : booking.getStatus().name());
                    row.put("bookedAt", booking.getBookedAt());
                    row.put("startedAt", booking.getStartedAt());
                    row.put("completedAt", booking.getCompletedAt());
                    row.put("agoraChannel", booking.getAgoraChannel());
                    row.put("paymentMethod", defaultText(booking.getPaymentMethod()));
                    row.put("transactionId", defaultText(booking.getTransactionId()));
                    row.put("totalPrice", booking.getTotalPrice());
                    row.put("slotSelectedByMobile", booking.getSlotSelectedByMobile());
                    return row;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "status", true,
                "count", rows.size(),
                "bookings", rows
        ));
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

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
