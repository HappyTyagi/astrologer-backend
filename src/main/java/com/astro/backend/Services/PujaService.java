package com.astro.backend.Services;


import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.ResponseDTO.PujaRescheduleItemResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private final EmailService emailService;
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

    public Map<String, Object> resendReceiptEmail(String orderId, ResendReceiptRequest request) {
        if (orderId == null || orderId.isBlank()) {
            throw new RuntimeException("Valid orderId is required");
        }
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final String toEmail = request.getEmail() == null ? "" : request.getEmail().trim();
        if (toEmail.isEmpty()) {
            throw new RuntimeException("Valid email is required");
        }

        final Long bookingId = parseBookingId(orderId);
        final PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + orderId));
        if (!Objects.equals(booking.getUserId(), request.getUserId())) {
            throw new RuntimeException("Order does not belong to this user");
        }

        final Puja puja = booking.getPujaId() == null
                ? null
                : pujaRepo.findById(booking.getPujaId()).orElse(null);
        final PujaSlot slot = booking.getSlotId() == null
                ? null
                : slotRepo.findById(booking.getSlotId()).orElse(null);

        final String subject = "Your Astrologer Puja Receipt - #" + bookingId;
        final String html = buildPujaReceiptHtml(booking, puja, slot);
        final byte[] pdf = buildPujaReceiptPdf(booking, puja, slot);
        final String fileName = "invoice.pdf";
        emailService.sendEmailWithAttachmentAsync(
                toEmail,
                subject,
                html,
                fileName,
                pdf,
                "application/pdf"
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Receipt mail request sent successfully. You will receive the email shortly.");
        response.put("queued", true);
        response.put("orderId", "PUJA-" + bookingId);
        response.put("email", toEmail);
        response.put("requestedAt", LocalDateTime.now());
        return response;
    }

    private Long parseBookingId(String orderId) {
        final String normalized = orderId.trim().toUpperCase();
        final String numeric = normalized.startsWith("PUJA-")
                ? normalized.substring(5).trim()
                : normalized;
        try {
            return Long.parseLong(numeric);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid puja orderId: " + orderId);
        }
    }

    private String buildPujaReceiptHtml(PujaBooking booking, Puja puja, PujaSlot slot) {
        final String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                ? "Puja Booking"
                : puja.getName();
        final String bookedAt = formatDateTime(booking.getBookedAt());
        final String slotTime = formatDateTime(slot == null ? null : slot.getSlotTime());
        final String status = booking.getStatus() == null ? "-" : booking.getStatus().name();
        final String paymentMethod = booking.getPaymentMethod() == null || booking.getPaymentMethod().isBlank()
                ? "-"
                : booking.getPaymentMethod();
        final String txnId = booking.getTransactionId() == null || booking.getTransactionId().isBlank()
                ? "-"
                : booking.getTransactionId();
        final double amount = booking.getTotalPrice() == null ? 0.0 : booking.getTotalPrice();
        final String amountText = formatMoney(amount, "INR");

        return """
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { margin:0; padding:0; background:#f4f6fa; font-family:Arial,sans-serif; color:#1f2533; }
                    .wrap { width:100%%; padding:22px 14px; }
                    .card { max-width:680px; margin:0 auto; background:#ffffff; border:1px solid #e5e8ef; border-radius:14px; overflow:hidden; }
                    .head { padding:20px 22px; background:linear-gradient(90deg,#1f2f73,#3146a3); color:#fff; }
                    .head h1 { margin:0; font-size:20px; letter-spacing:0.2px; }
                    .head p { margin:8px 0 0 0; font-size:13px; color:#d8e0ff; }
                    .body { padding:20px 22px; }
                    .pill { display:inline-block; border-radius:999px; padding:6px 11px; font-size:12px; font-weight:700; background:#eef4ff; color:#26439a; }
                    table { width:100%%; border-collapse:collapse; margin-top:12px; }
                    th, td { padding:11px 8px; border-bottom:1px solid #edf0f5; font-size:13px; }
                    th { text-align:left; color:#586174; background:#f8faff; font-weight:700; }
                    td:last-child, th:last-child { text-align:right; }
                    .foot { margin-top:14px; font-size:12px; color:#6e7583; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="head">
                        <h1>Puja Booking Receipt</h1>
                        <p>Thank you for booking with Astrologer. Your invoice PDF is attached with this email.</p>
                      </div>
                      <div class="body">
                        <span class="pill">%s</span>
                        <table>
                          <thead>
                            <tr><th>Field</th><th>Value</th></tr>
                          </thead>
                          <tbody>
                            <tr><td>Order ID</td><td>PUJA-%d</td></tr>
                            <tr><td>Puja Name</td><td>%s</td></tr>
                            <tr><td>Booked At</td><td>%s</td></tr>
                            <tr><td>Slot Time</td><td>%s</td></tr>
                            <tr><td>Payment Method</td><td>%s</td></tr>
                            <tr><td>Transaction ID</td><td>%s</td></tr>
                            <tr><td>Total Amount</td><td><strong>%s</strong></td></tr>
                          </tbody>
                        </table>
                        <div class="foot">For support, reply to this email. Keep this receipt for your records.</div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(status),
                booking.getId() == null ? 0L : booking.getId(),
                escapeHtml(pujaName),
                escapeHtml(bookedAt),
                escapeHtml(slotTime),
                escapeHtml(paymentMethod),
                escapeHtml(txnId),
                escapeHtml(amountText)
        );
    }

    private byte[] buildPujaReceiptPdf(PujaBooking booking, Puja puja, PujaSlot slot) {
        final String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                ? "Puja Booking"
                : puja.getName();
        final String orderId = "PUJA-" + (booking.getId() == null ? 0L : booking.getId());
        final String bookedAt = formatDateTime(booking.getBookedAt());
        final String slotTime = formatDateTime(slot == null ? null : slot.getSlotTime());
        final String paymentMethod = booking.getPaymentMethod() == null || booking.getPaymentMethod().isBlank()
                ? "-"
                : booking.getPaymentMethod();
        final String txnId = booking.getTransactionId() == null || booking.getTransactionId().isBlank()
                ? "-"
                : booking.getTransactionId();
        final String status = booking.getStatus() == null ? "-" : booking.getStatus().name();
        final double amount = booking.getTotalPrice() == null ? 0.0 : booking.getTotalPrice();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            Font h1 = new Font(Font.HELVETICA, 19, Font.BOLD, new Color(28, 40, 100));
            Font label = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(82, 88, 100));
            Font value = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(31, 37, 51));
            Font valueBold = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 27, 80));

            document.add(new Paragraph("Puja Booking Invoice", h1));
            document.add(new Paragraph("Thank you for booking with Astrologer.", value));
            document.add(Chunk.NEWLINE);

            PdfPTable meta = new PdfPTable(new float[]{2.2f, 3.8f});
            meta.setWidthPercentage(100);
            meta.addCell(pdfLabelCell("Order ID", label));
            meta.addCell(pdfValueCell(orderId, value));
            meta.addCell(pdfLabelCell("Status", label));
            meta.addCell(pdfValueCell(status, value));
            meta.addCell(pdfLabelCell("Puja Name", label));
            meta.addCell(pdfValueCell(pujaName, value));
            meta.addCell(pdfLabelCell("Booked At", label));
            meta.addCell(pdfValueCell(bookedAt, value));
            meta.addCell(pdfLabelCell("Slot Time", label));
            meta.addCell(pdfValueCell(slotTime, value));
            meta.addCell(pdfLabelCell("Payment Method", label));
            meta.addCell(pdfValueCell(paymentMethod, value));
            meta.addCell(pdfLabelCell("Transaction ID", label));
            meta.addCell(pdfValueCell(txnId, value));
            meta.addCell(pdfLabelCell("Total Amount", label));
            meta.addCell(pdfValueCell(formatMoney(amount, "INR"), valueBold));

            document.add(meta);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Keep this invoice for your records.", value));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate puja receipt PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell pdfLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(9f);
        cell.setBorderColor(new Color(227, 231, 239));
        cell.setBackgroundColor(new Color(248, 250, 255));
        return cell;
    }

    private PdfPCell pdfValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "-" : text, font));
        cell.setPadding(9f);
        cell.setBorderColor(new Color(227, 231, 239));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String formatMoney(double amount, String currency) {
        final String c = currency == null ? "INR" : currency.toUpperCase();
        final String symbol = "INR".equals(c) ? "INR " : c + " ";
        return symbol + String.format(Locale.US, "%.2f", amount);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
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
