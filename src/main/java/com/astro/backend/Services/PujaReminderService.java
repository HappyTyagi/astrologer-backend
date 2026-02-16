package com.astro.backend.Services;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PujaReminderService {

    private final PujaBookingRepository pujaBookingRepository;
    private final PujaSlotRepository pujaSlotRepository;
    private final PujaRepository pujaRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final EmailService emailService;
    private final FcmPushService fcmPushService;

    private static final DateTimeFormatter SLOT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Duration TRIGGER_WINDOW = Duration.ofMinutes(3);
    private static final List<ReminderStage> REMINDER_STAGES = List.of(
            new ReminderStage("3D", Duration.ofDays(3), "3 days"),
            new ReminderStage("1D", Duration.ofDays(1), "1 day"),
            new ReminderStage("6H", Duration.ofHours(6), "6 hours"),
            new ReminderStage("2H", Duration.ofHours(2), "2 hours"),
            new ReminderStage("30M", Duration.ofMinutes(30), "30 minutes"),
            new ReminderStage("5M", Duration.ofMinutes(5), "5 minutes")
    );

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void sendUpcomingPujaReminders() {
        final List<PujaBooking> candidates = pujaBookingRepository.findByStatusIn(
                List.of(PujaBooking.BookingStatus.CONFIRMED, PujaBooking.BookingStatus.PENDING)
        );
        if (candidates.isEmpty()) {
            return;
        }

        final LocalDateTime now = LocalDateTime.now();
        for (PujaBooking booking : candidates) {
            try {
                processReminder(booking, now);
            } catch (Exception e) {
                log.error("Failed to process puja reminder for bookingId={}", booking.getId(), e);
            }
        }
    }

    private void processReminder(PujaBooking booking, LocalDateTime now) {
        final PujaSlot slot = booking.getSlotId() == null ? null : pujaSlotRepository.findById(booking.getSlotId()).orElse(null);
        if (slot == null || slot.getSlotTime() == null) {
            return;
        }
        final LocalDateTime slotTime = slot.getSlotTime();
        if (slotTime.isBefore(now.minusMinutes(1))) {
            return;
        }

        final MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
        final Puja puja = booking.getPujaId() == null ? null : pujaRepository.findById(booking.getPujaId()).orElse(null);
        final String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                ? "Puja Booking"
                : puja.getName();
        final String meetingLink = booking.getMeetingLink() == null || booking.getMeetingLink().isBlank()
                ? "https://meet.google.com/new"
                : booking.getMeetingLink();
        final String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
        final String fcmToken = extractFcmToken(profile);
        final Set<String> sentStages = parseSentStages(booking.getNotificationStatus());

        boolean changed = false;
        for (ReminderStage stage : REMINDER_STAGES) {
            if (sentStages.contains(stage.code())) {
                continue;
            }
            final LocalDateTime triggerAt = slotTime.minus(stage.beforeSlot());
            if (!isWithinTriggerWindow(now, triggerAt)) {
                continue;
            }

            final String subject = "Puja Reminder: " + pujaName + " starts in " + stage.label();
            final String html = """
                <html>
                <body style="font-family:Arial,sans-serif;background:#f6f8fc;padding:16px;">
                  <div style="max-width:680px;margin:auto;background:#fff;border:1px solid #e6ebf2;border-radius:12px;padding:18px;">
                    <h2 style="margin:0 0 8px 0;color:#1f2f73;">Puja Reminder</h2>
                    <p style="margin:0 0 12px 0;">Your puja session will start in approximately <strong>%s</strong>.</p>
                    <table style="width:100%%;border-collapse:collapse;">
                      <tr><td style="padding:8px 0;color:#546074;">Booking ID</td><td style="padding:8px 0;text-align:right;">PUJA-%d</td></tr>
                      <tr><td style="padding:8px 0;color:#546074;">Puja Name</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                      <tr><td style="padding:8px 0;color:#546074;">Slot Time</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                      <tr><td style="padding:8px 0;color:#546074;">Meeting Link</td><td style="padding:8px 0;text-align:right;"><a href="%s">Join Google Meet</a></td></tr>
                    </table>
                    <p style="margin-top:14px;color:#6a7383;">Please join on time and keep your booking reference handy.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(stage.label()),
                booking.getId() == null ? 0L : booking.getId(),
                escapeHtml(pujaName),
                escapeHtml(slotTime.format(SLOT_FORMATTER)),
                escapeHtml(meetingLink)
            );

            if (!toEmail.isEmpty()) {
                emailService.sendEmailAsync(toEmail, subject, html);
            }

            if (!fcmToken.isEmpty()) {
                String pushBody = "Your " + pujaName + " booking starts in " + stage.label() + ". Tap to join the session.";
                fcmPushService.sendToTokenAsync(
                        fcmToken,
                        "Puja Reminder",
                        pushBody,
                        "PUJA_REMINDER",
                        null,
                        null,
                        "{\"bookingId\":\"PUJA-" + (booking.getId() == null ? 0L : booking.getId()) + "\",\"meetingLink\":\"" + escapeJson(meetingLink) + "\"}"
                );
            }

            if (!toEmail.isEmpty() || !fcmToken.isEmpty()) {
                sentStages.add(stage.code());
                changed = true;
                log.info("Queued puja reminder. bookingId={}, stage={}, emailSent={}, pushSent={}",
                        booking.getId(), stage.code(), !toEmail.isEmpty(), !fcmToken.isEmpty());
            } else {
                log.warn("Puja reminder skipped: no email/fcm token. bookingId={}, stage={}", booking.getId(), stage.code());
            }
        }

        if (changed) {
            booking.setNotificationStatus(buildStatusValue(sentStages));
            booking.setReminderSentAt(now);
            pujaBookingRepository.save(booking);
        }
    }

    private boolean isWithinTriggerWindow(LocalDateTime now, LocalDateTime triggerAt) {
        return !now.isBefore(triggerAt) && now.isBefore(triggerAt.plus(TRIGGER_WINDOW));
    }

    private String extractFcmToken(MobileUserProfile profile) {
        if (profile == null) {
            return "";
        }
        if (profile.getFcmToken() != null && !profile.getFcmToken().trim().isEmpty()) {
            return profile.getFcmToken().trim();
        }
        if (profile.getDeviceToken() != null && !profile.getDeviceToken().trim().isEmpty()) {
            return profile.getDeviceToken().trim();
        }
        return "";
    }

    private Set<String> parseSentStages(String notificationStatus) {
        if (notificationStatus == null || notificationStatus.isBlank()) {
            return new LinkedHashSet<>();
        }
        int idx = notificationStatus.indexOf("STAGES=");
        if (idx < 0) {
            return new LinkedHashSet<>();
        }
        String part = notificationStatus.substring(idx + 7).trim();
        if (part.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(part.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildStatusValue(Set<String> sentStages) {
        if (sentStages.isEmpty()) {
            return "PENDING";
        }
        return "PENDING|STAGES=" + String.join(",", sentStages);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record ReminderStage(String code, Duration beforeSlot, String label) {}
}
