package com.astro.backend.Services;

import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PujaBookingNotificationService {

    private static final DateTimeFormatter SLOT_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final PujaBookingRepository pujaBookingRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final FcmPushService fcmPushService;

    public void notifyAdminAndPandit(PujaBooking booking, Puja puja, PujaSlot slot) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        final String channel = ensureAgoraChannel(booking);
        final String pujaName = puja == null || isBlank(puja.getName()) ? "Puja Booking" : puja.getName().trim();
        final String slotText = slot == null || slot.getSlotTime() == null
                ? "Slot pending"
                : slot.getSlotTime().format(SLOT_FORMATTER);

        final String title = "New Puja Booking";
        final String body = pujaName + " booked for " + slotText + ".";
        final String actionData = buildActionData(booking, puja, slot, channel);

        // Notify Admin(s)
        final List<User> admins = userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN);
        for (User admin : admins) {
            pushToUser(admin, title, body, actionData);
        }

        // Notify assigned Pandit/Astrologer
        final Long astrologerId = puja == null ? null : puja.getAstrologerId();
        if (astrologerId != null && astrologerId > 0) {
            userRepository.findById(astrologerId).ifPresent(pandit -> pushToUser(pandit, title, body, actionData));
        }
    }

    private void pushToUser(User target, String title, String body, String actionData) {
        if (target == null || target.getId() == null || target.getId() <= 0) {
            return;
        }
        final MobileUserProfile profile = mobileUserProfileRepository.findByUserId(target.getId()).orElse(null);
        if (profile == null) {
            return;
        }
        final String token = safe(profile.getFcmToken());
        if (token.isBlank()) {
            return;
        }
        try {
            fcmPushService.sendToTokenAsync(
                    token,
                    title,
                    body,
                    "PUJA_BOOKED",
                    null,
                    null,
                    actionData
            );
        } catch (Exception e) {
            log.warn("Failed to send PUJA_BOOKED push. userId={}, reason={}", target.getId(), e.getMessage());
        }
    }

    private String ensureAgoraChannel(PujaBooking booking) {
        final String existing = safe(booking.getAgoraChannel());
        if (!existing.isBlank()) {
            return existing;
        }
        final String generated = "puja_" + booking.getId();
        booking.setAgoraChannel(generated);
        pujaBookingRepository.save(booking);
        return generated;
    }

    private String buildActionData(PujaBooking booking, Puja puja, PujaSlot slot, String channel) {
        final String slotTime = slot == null || slot.getSlotTime() == null
                ? ""
                : slot.getSlotTime().toString();
        return String.format(
                Locale.ROOT,
                "{\"source\":\"PUJA_BOOKING\",\"bookingId\":%d,\"pujaId\":%d,\"slotId\":%s,\"slotTime\":\"%s\",\"agoraChannel\":\"%s\",\"callType\":\"video\"}",
                booking.getId(),
                booking.getPujaId() == null ? 0 : booking.getPujaId(),
                booking.getSlotId() == null ? "null" : booking.getSlotId().toString(),
                escapeJson(slotTime),
                escapeJson(channel)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
