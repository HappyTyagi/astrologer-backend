package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.ResponseDTO.BirthdayNotificationResponse;
import com.astro.backend.Services.BirthdayNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller for birthday notifications
 * Users receive this when they open the app
 */
@RestController
@RequestMapping("/api/mobile/birthday-notifications")
@RequiredArgsConstructor
public class BirthdayNotificationController {

    private final BirthdayNotificationService birthdayNotificationService;

    /**
     * Get pending birthday notifications for user (App opens)
     * Call this endpoint when user opens the mobile app
     */
    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<BirthdayNotificationResponse>> getPendingNotifications(
            @PathVariable Long userId) {
        try {
            List<BirthdayNotificationResponse> notifications = birthdayNotificationService.getUserBirthdayNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get all birthday notifications for user (History)
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<BirthdayNotificationResponse>> getAllNotifications(
            @PathVariable Long userId) {
        try {
            List<BirthdayNotificationResponse> notifications = birthdayNotificationService.getAllUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Mark birthday notification as viewed
     */
    @PutMapping("/mark-viewed/{notificationId}")
    public ResponseEntity<BirthdayNotificationResponse> markAsViewed(
            @PathVariable Long notificationId) {
        try {
            BirthdayNotificationResponse response = birthdayNotificationService.markAsViewed(notificationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(BirthdayNotificationResponse.builder()
                    .statusCode(false)
                    .message_resp(e.getMessage())
                    .build());
        }
    }
}
