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
import java.util.List;

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
}
