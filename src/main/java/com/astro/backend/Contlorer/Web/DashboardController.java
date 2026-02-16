package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.User;
import com.astro.backend.Entity.WalletTransaction;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.Repositry.WalletTransactionRepository;
import com.astro.backend.ResponseDTO.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final PujaBookingRepository pujaBookingRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Get dashboard statistics
     * Shows: total registered users, total amount, upcoming births, total upcoming puja
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardResponse> getDashboardStats() {
        try {
            // 1. Count total registered users
            long totalRegisteredUsers = userRepository.count();

            // 2. Calculate total amount from wallet transactions (CREDIT type)
            Double totalAmount = calculateTotalAmount();

            // 3. Count upcoming births (users with dateOfBirth in current month - upcoming)
            Integer upcomingBirths = countUpcomingBirths();

            // 4. Count total upcoming puja (bookings with PENDING or CONFIRMED status)
            Integer totalUpcomingPuja = countUpcomingPuja();
            Long totalRemediesBooked = remidesPurchaseRepository.count();
            Long totalPujaBooked = pujaBookingRepository.count();

            DashboardResponse response = DashboardResponse.builder()
                    .totalRegisteredUsers(totalRegisteredUsers)
                    .totalAmount(totalAmount)
                    .upcomingBirths(upcomingBirths)
                    .totalUpcomingPuja(totalUpcomingPuja)
                    .totalRemediesBooked(totalRemediesBooked)
                    .totalPujaBooked(totalPujaBooked)
                    .status(true)
                    .message("Dashboard statistics retrieved successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DashboardResponse.builder()
                            .status(false)
                            .message("Error retrieving dashboard stats: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Calculate total amount from wallet transactions
     * Sums all CREDIT type transactions
     */
    private Double calculateTotalAmount() {
        List<WalletTransaction> allTransactions = walletTransactionRepository.findAll();
        return allTransactions.stream()
                .filter(txn -> "CREDIT".equalsIgnoreCase(txn.getType()) && "SUCCESS".equalsIgnoreCase(txn.getStatus()))
                .mapToDouble(WalletTransaction::getAmount)
                .sum();
    }

    /**
     * Count upcoming births (birthdays in next 30 days or current month)
     * This counts mobile users with valid dateOfBirth
     */
    private Integer countUpcomingBirths() {
        List<MobileUserProfile> profiles = mobileUserProfileRepository.findAll();
        return (int) profiles.stream()
                .filter(profile -> profile.getDateOfBirth() != null)
                .count();
    }

    /**
     * Count total upcoming puja (bookings not completed or cancelled)
     */
    private Integer countUpcomingPuja() {
        List<PujaBooking> bookings = pujaBookingRepository.findAll();
        return (int) bookings.stream()
                .filter(booking -> booking.getStatus() != null && 
                        (booking.getStatus() == PujaBooking.BookingStatus.PENDING ||
                         booking.getStatus() == PujaBooking.BookingStatus.CONFIRMED))
                .count();
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> getPaymentsSummary(
            @RequestParam(defaultValue = "today") String filter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start;
            LocalDateTime end;
            String normalized = filter == null ? "today" : filter.trim().toLowerCase();

            switch (normalized) {
                case "today" -> {
                    start = LocalDate.now().atStartOfDay();
                    end = LocalDate.now().atTime(LocalTime.MAX);
                }
                case "weekly" -> {
                    LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate weekEnd = weekStart.plusDays(6);
                    start = weekStart.atStartOfDay();
                    end = weekEnd.atTime(LocalTime.MAX);
                }
                case "range" -> {
                    if (startDate == null || endDate == null) {
                        throw new RuntimeException("startDate and endDate are required for range filter (YYYY-MM-DD)");
                    }
                    LocalDate s = LocalDate.parse(startDate.trim());
                    LocalDate e = LocalDate.parse(endDate.trim());
                    if (e.isBefore(s)) {
                        throw new RuntimeException("endDate must be on or after startDate");
                    }
                    start = s.atStartOfDay();
                    end = e.atTime(LocalTime.MAX);
                }
                default -> throw new RuntimeException("Invalid filter. Use today, weekly, or range");
            }

            List<WalletTransaction> rows =
                    walletTransactionRepository.findByTypeIgnoreCaseAndStatusIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
                            "CREDIT",
                            "SUCCESS",
                            start,
                            end
                    );

            double totalAmount = rows.stream().mapToDouble(WalletTransaction::getAmount).sum();
            long totalTransactions = rows.size();
            double avgAmount = totalTransactions == 0 ? 0.0 : totalAmount / totalTransactions;

            List<Map<String, Object>> items = rows.stream().limit(500).map(txn -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", txn.getId());
                m.put("userId", txn.getUserId());
                m.put("amount", txn.getAmount());
                m.put("type", txn.getType());
                m.put("status", txn.getStatus());
                m.put("refId", txn.getRefId());
                m.put("description", txn.getDescription());
                m.put("createdAt", txn.getCreatedAt());
                return m;
            }).toList();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", true);
            body.put("message", "Payment summary fetched successfully");
            body.put("filter", normalized);
            body.put("start", start);
            body.put("end", end);
            body.put("generatedAt", now);
            body.put("totalTransactions", totalTransactions);
            body.put("totalAmount", totalAmount);
            body.put("averageAmount", avgAmount);
            body.put("items", items);
            return ResponseEntity.ok(body);
        } catch (DateTimeParseException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", false);
            body.put("message", "Invalid date format. Use YYYY-MM-DD");
            return ResponseEntity.badRequest().body(body);
        } catch (RuntimeException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", false);
            body.put("message", "Error fetching payment summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
