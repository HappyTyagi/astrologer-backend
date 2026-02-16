package com.astro.backend.Services;


import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.ResponseDTO.PujaRescheduleItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PujaService {

    private final PujaRepository pujaRepo;
    private final PujaSlotRepository slotRepo;
    private final PujaBookingRepository bookingRepo;
    private final AddressRepository addressRepository;
    private final WalletService walletService;
    private final OrderHistoryService orderHistoryService;
    private static final LocalTime DEFAULT_DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_DAY_END = LocalTime.of(20, 0);
    private static final int DEFAULT_GAP_MINUTES = 30;

    public PujaBooking bookPuja(
            Long userId,
            Long pujaId,
            Long slotId,
            Long addressId,
            String paymentMethod,
            String transactionId,
            Boolean useWallet
    ) {

        if (addressId == null || addressId <= 0) {
            throw new RuntimeException("Valid addressId is required");
        }

        Puja puja = pujaRepo.findById(pujaId)
                .orElseThrow(() -> new RuntimeException("Invalid Puja"));

        PujaSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Invalid Slot"));
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        if (slot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
            throw new RuntimeException("Slot not available");
        }

        final String normalizedMethod = paymentMethod == null ? "WALLET" : paymentMethod.trim().toUpperCase();
        final boolean wantsWallet = Boolean.TRUE.equals(useWallet);
        final boolean isWalletPayment = "WALLET".equals(normalizedMethod);
        final boolean isGatewayPayment = "GATEWAY".equals(normalizedMethod)
                || "UPI".equals(normalizedMethod)
                || "CARD".equals(normalizedMethod)
                || "NETBANKING".equals(normalizedMethod);

        if (!isWalletPayment && !isGatewayPayment) {
            throw new RuntimeException("Invalid paymentMethod. Use WALLET or GATEWAY.");
        }

        String finalTransactionId = transactionId == null ? "" : transactionId.trim();
        String finalPaymentMethod = normalizedMethod;
        final double pujaAmount = puja.getPrice();
        if (isWalletPayment) {
            boolean debited = walletService.debit(
                    userId,
                    pujaAmount,
                    "PUJA_BOOKING",
                    "Puja booking: " + puja.getName()
            );

            if (!debited) {
                throw new RuntimeException("Insufficient wallet balance. Please add money to wallet or continue with gateway payment.");
            }
            if (finalTransactionId.isEmpty()) {
                finalTransactionId = "WALLET-" + UUID.randomUUID();
            }
            finalPaymentMethod = "WALLET";
        } else {
            double walletUsed = 0.0;
            if (wantsWallet) {
                Wallet wallet = walletService.getWallet(userId);
                double balance = wallet.getBalance();
                if (balance > 0) {
                    walletUsed = walletService.debitUpTo(
                            userId,
                            pujaAmount,
                            "PUJA_BOOKING",
                            "Puja booking (wallet part): " + puja.getName()
                    );
                }
            }

            final double remaining = Math.max(0.0, pujaAmount - walletUsed);
            if (remaining > 0 && finalTransactionId.isEmpty()) {
                throw new RuntimeException("Gateway transactionId is required for remaining amount.");
            }
            if (remaining <= 0) {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "WALLET-" + UUID.randomUUID();
                }
                finalPaymentMethod = "WALLET";
            } else {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "GW-" + UUID.randomUUID();
                }
                finalPaymentMethod = walletUsed > 0 ? "WALLET+GATEWAY" : normalizedMethod;
            }
        }

        // Update slot
        slot.setStatus(PujaSlot.SlotStatus.BOOKED);
        slotRepo.save(slot);

        // Create booking
        PujaBooking booking = PujaBooking.builder()
                .userId(userId)
                .pujaId(pujaId)
                .slotId(slotId)
                .address(address)
                .bookedAt(LocalDateTime.now())
                .status(PujaBooking.BookingStatus.CONFIRMED)
                .totalPrice(puja.getPrice())
                .paymentMethod(finalPaymentMethod)
                .transactionId(finalTransactionId)
                .build();

        PujaBooking savedBooking = bookingRepo.save(booking);
        orderHistoryService.recordPujaBooking(savedBooking, puja, slot);
        return savedBooking;
    }

    public Map<String, Object> generateSlotsFromMaster(PujaSlotMasterRequest request) {
        if (request.getPujaId() == null) {
            throw new RuntimeException("pujaId is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("startDate and endDate are required in YYYY-MM-DD");
        }
        final Puja puja = pujaRepo.findById(request.getPujaId())
                .orElseThrow(() -> new RuntimeException("Invalid pujaId"));

        final int durationMinutes = puja.getDurationMinutes();
        if (durationMinutes <= 0) {
            throw new RuntimeException("Puja durationMinutes must be greater than 0");
        }

        final int gapMinutes = DEFAULT_GAP_MINUTES;
        if (gapMinutes < 0) {
            throw new RuntimeException("gapMinutes cannot be negative");
        }

        final int maxBookings = request.getMaxBookings() == null || request.getMaxBookings() <= 0
                ? 1
                : request.getMaxBookings();

        final LocalDate startDate;
        final LocalDate endDate;
        final LocalTime dayStart;
        final LocalTime dayEnd;
        try {
            startDate = LocalDate.parse(request.getStartDate().trim());
            endDate = LocalDate.parse(request.getEndDate().trim());
            dayStart = DEFAULT_DAY_START;
            dayEnd = DEFAULT_DAY_END;
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date/time format. Use startDate/endDate=YYYY-MM-DD and dayStartTime/dayEndTime=HH:mm");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("endDate must be on or after startDate");
        }
        if (!dayEnd.isAfter(dayStart)) {
            throw new RuntimeException("dayEndTime must be after dayStartTime");
        }

        long createdCount = 0;
        long duplicateCount = 0;
        long tooShortWindowDays = 0;
        List<PujaSlot> toSave = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalDateTime cursor = d.atTime(dayStart);
            final LocalDateTime dayClose = d.atTime(dayEnd);

            if (cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                tooShortWindowDays++;
                continue;
            }

            while (!cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                if (slotRepo.existsByPujaIdAndSlotTime(request.getPujaId(), cursor)) {
                    duplicateCount++;
                } else {
                    toSave.add(
                            PujaSlot.builder()
                                    .pujaId(request.getPujaId())
                                    .slotTime(cursor)
                                    .status(PujaSlot.SlotStatus.AVAILABLE)
                                    .astrologerId(request.getAstrologerId())
                                    .maxBookings(maxBookings)
                                    .currentBookings(0)
                                    .isRecurring(Boolean.TRUE.equals(request.getIsRecurring()))
                                    .recurringPattern(request.getRecurringPattern())
                                    .isActive(true)
                                    .build()
                    );
                    createdCount++;
                }
                cursor = cursor.plusMinutes((long) durationMinutes + gapMinutes);
            }
        }

        if (!toSave.isEmpty()) {
            slotRepo.saveAll(toSave);
        }

        return Map.of(
                "status", true,
                "message", "Puja slots generated successfully",
                "pujaId", request.getPujaId(),
                "pujaDurationMinutes", durationMinutes,
                "gapMinutes", gapMinutes,
                "createdSlots", createdCount,
                "duplicateSlotsSkipped", duplicateCount,
                "daysWithNoWindow", tooShortWindowDays,
                "fromDate", startDate.toString(),
                "toDate", endDate.toString()
        );
    }

    public void ensureDefaultSlotsForPuja(Long pujaId) {
        if (pujaId == null || pujaId <= 0) {
            return;
        }
        final Puja puja = pujaRepo.findById(pujaId).orElse(null);
        if (puja == null) {
            return;
        }
        final int durationMinutes = puja.getDurationMinutes() <= 0 ? 60 : puja.getDurationMinutes();

        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = startDate.plusDays(30);
        final int maxBookings = 1;

        List<PujaSlot> toSave = new ArrayList<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            LocalDateTime cursor = day.atTime(DEFAULT_DAY_START);
            final LocalDateTime dayClose = day.atTime(DEFAULT_DAY_END);

            while (!cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                if (!slotRepo.existsByPujaIdAndSlotTime(pujaId, cursor)) {
                    toSave.add(PujaSlot.builder()
                            .pujaId(pujaId)
                            .slotTime(cursor)
                            .status(PujaSlot.SlotStatus.AVAILABLE)
                            .maxBookings(maxBookings)
                            .currentBookings(0)
                            .isRecurring(false)
                            .recurringPattern(null)
                            .isActive(true)
                            .build());
                }
                cursor = cursor.plusMinutes((long) durationMinutes + DEFAULT_GAP_MINUTES);
            }
        }
        if (!toSave.isEmpty()) {
            slotRepo.saveAll(toSave);
        }
    }

    public List<PujaRescheduleItemResponse> getUserRescheduleBookings(Long userId) {
        List<PujaBooking> bookings = bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        return bookings.stream()
                .map(booking -> {
                    Puja puja = booking.getPujaId() == null
                            ? null
                            : pujaRepo.findById(booking.getPujaId()).orElse(null);
                    PujaSlot currentSlot = booking.getSlotId() == null
                            ? null
                            : slotRepo.findById(booking.getSlotId()).orElse(null);

                    boolean statusEligible = booking.getStatus() == PujaBooking.BookingStatus.CONFIRMED
                            || booking.getStatus() == PujaBooking.BookingStatus.PENDING;

                    LocalDateTime cutoff = currentSlot != null && currentSlot.getSlotTime() != null
                            ? currentSlot.getSlotTime().minusDays(1)
                            : null;

                    boolean timeEligible = cutoff != null && !now.isAfter(cutoff);
                    boolean canReschedule = statusEligible && timeEligible;

                    String msg;
                    if (!statusEligible) {
                        msg = "Reschedule allowed only for confirmed/pending bookings.";
                    } else if (currentSlot == null || currentSlot.getSlotTime() == null) {
                        msg = "Current slot details unavailable.";
                    } else if (!timeEligible) {
                        msg = "Rescheduling window closed (allowed until 1 day before slot).";
                    } else {
                        msg = "Eligible for rescheduling.";
                    }

                    return PujaRescheduleItemResponse.builder()
                            .bookingId(booking.getId())
                            .pujaId(booking.getPujaId())
                            .pujaName(puja != null ? puja.getName() : "Puja")
                            .currentSlotId(booking.getSlotId())
                            .currentSlotTime(currentSlot != null ? currentSlot.getSlotTime() : null)
                            .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                            .totalPrice(booking.getTotalPrice())
                            .canReschedule(canReschedule)
                            .rescheduleAllowedTill(cutoff)
                            .rescheduleMessage(msg)
                            .build();
                })
                .toList();
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value); // HH:mm or HH:mm:ss
        } catch (DateTimeParseException ignored) {
            if (value != null && value.length() == 5) {
                return LocalTime.parse(value + ":00");
            }
            throw ignored;
        }
    }

    public PujaBooking reschedulePuja(Long userId, Long bookingId, Long newSlotId) {
        PujaBooking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new RuntimeException("Booking not found for user"));

        if (booking.getStatus() != PujaBooking.BookingStatus.CONFIRMED
                && booking.getStatus() != PujaBooking.BookingStatus.PENDING) {
            throw new RuntimeException("Only confirmed/pending booking can be rescheduled");
        }

        PujaSlot currentSlot = slotRepo.findById(booking.getSlotId())
                .orElseThrow(() -> new RuntimeException("Current slot not found"));
        LocalDateTime cutoff = currentSlot.getSlotTime().minusDays(1);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new RuntimeException("Rescheduling not allowed within 1 day of puja slot");
        }

        PujaSlot newSlot = slotRepo.findById(newSlotId)
                .orElseThrow(() -> new RuntimeException("New slot not found"));

        if (!newSlot.getPujaId().equals(booking.getPujaId())) {
            throw new RuntimeException("Selected slot does not belong to this puja");
        }
        if (newSlot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
            throw new RuntimeException("Selected slot is already filled");
        }
        if (newSlot.getSlotTime() == null || !newSlot.getSlotTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Selected slot must be in future");
        }

        // Free current slot and reserve new slot
        currentSlot.setStatus(PujaSlot.SlotStatus.AVAILABLE);
        slotRepo.save(currentSlot);

        newSlot.setStatus(PujaSlot.SlotStatus.BOOKED);
        slotRepo.save(newSlot);

        booking.setSlotId(newSlot.getId());
        booking.setStatus(PujaBooking.BookingStatus.CONFIRMED);
        return bookingRepo.save(booking);
    }
}
