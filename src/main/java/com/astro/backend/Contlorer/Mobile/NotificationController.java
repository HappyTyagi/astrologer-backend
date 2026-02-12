package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.Notification;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.NotificationRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.SendNotificationRequest;
import com.astro.backend.RequestDTO.TestNotificationRequest;
import com.astro.backend.ResponseDTO.NotificationResponse;
import com.astro.backend.Services.FcmPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final FcmPushService fcmPushService;

    /**
     * Test notification endpoint
     * Check label validity and send test notification
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testNotification(@Valid @RequestBody TestNotificationRequest request) {
        try {
            String label = request.getLabel().trim();
            String testMessage = request.getTestMessage().trim();

            // Validate label format
            if (!isValidLabel(label)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Invalid label format. Label must be alphanumeric with hyphens/underscores only",
                        "label", label
                ));
            }

            // Send test notification
            Map<String, Object> response = new HashMap<>();
            response.put("status", true);
            response.put("message", "Test notification processed successfully");
            response.put("label", label);
            response.put("testMessage", testMessage);
            response.put("timestamp", LocalDateTime.now());
            response.put("deliveryStatus", "SENT");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", false,
                            "message", "Error processing test notification: " + e.getMessage()
                    ));
        }
    }

    /**
     * Send notification to user with image banner
     * Accepts title, message, image URL, and other notification details
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        try {
            // Validate user exists
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));
            Notification.NotificationType notificationType =
                    request.getType() != null ? request.getType() : Notification.NotificationType.PROMO;

            // If user opted out from promotional notifications, ignore promo sends
            if (notificationType == Notification.NotificationType.PROMO
                    && Boolean.FALSE.equals(user.getPromotionalNotificationsEnabled())) {
                return ResponseEntity.ok(
                        NotificationResponse.builder()
                                .userId(request.getUserId())
                                .type(notificationType.toString())
                                .status(false)
                                .message("User has opted out from promotional notifications")
                                .build()
                );
            }

            String fcmToken = mobileUserProfileRepository.findByUserId(request.getUserId())
                    .map(p -> p.getFcmToken() == null ? "" : p.getFcmToken().trim())
                    .orElse("");

            String deliveryStatus = "FAILED";
            String failureReason = null;
            if (fcmToken.isBlank()) {
                failureReason = "Missing FCM token";
            } else {
                FcmPushService.PushResult pushResult = fcmPushService.sendToToken(
                        fcmToken,
                        request.getTitle().trim(),
                        request.getMessage().trim(),
                        notificationType.name(),
                        request.getImageUrl(),
                        request.getActionUrl(),
                        request.getActionData()
                );
                if (pushResult.isSuccess()) {
                    deliveryStatus = "SENT";
                } else {
                    failureReason = pushResult.getReason();
                }
            }

            // Build notification object
            Notification notification = Notification.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle().trim())
                    .message(request.getMessage().trim())
                    .type(notificationType)
                    .imageUrl(request.getImageUrl())
                    .actionUrl(request.getActionUrl())
                    .actionData(request.getActionData())
                    .isRead(false)
                    .deliveryStatus(deliveryStatus)
                    .failureReason(failureReason)
                    .sentAt("SENT".equals(deliveryStatus) ? LocalDateTime.now() : null)
                    .build();

            // Save notification to database
            Notification savedNotification = notificationRepository.save(notification);

            // Build response
            NotificationResponse response = NotificationResponse.builder()
                    .id(savedNotification.getId())
                    .userId(savedNotification.getUserId())
                    .title(savedNotification.getTitle())
                    .message(savedNotification.getMessage())
                    .type(savedNotification.getType().toString())
                    .imageUrl(savedNotification.getImageUrl())
                    .deliveryStatus(savedNotification.getDeliveryStatus())
                    .isRead(savedNotification.getIsRead())
                    .status("SENT".equals(savedNotification.getDeliveryStatus()))
                    .message("SENT".equals(savedNotification.getDeliveryStatus())
                            ? "Notification sent successfully to user"
                            : "Notification saved, but push send failed: " + savedNotification.getFailureReason())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NotificationResponse.builder()
                            .status(false)
                            .message("Failed to send notification: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Validate label format
     * Allowed: alphanumeric, hyphens, underscores
     */
    private boolean isValidLabel(String label) {
        return label != null && !label.isEmpty() && label.matches("^[a-zA-Z0-9_-]+$");
    }
}
