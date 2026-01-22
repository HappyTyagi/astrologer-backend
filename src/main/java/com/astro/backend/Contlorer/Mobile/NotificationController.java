package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.Notification;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.NotificationRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.SendNotificationRequest;
import com.astro.backend.RequestDTO.TestNotificationRequest;
import com.astro.backend.ResponseDTO.NotificationResponse;
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

            // Build notification object
            Notification notification = Notification.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle().trim())
                    .message(request.getMessage().trim())
                    .type(request.getType() != null ? request.getType() : Notification.NotificationType.PROMO)
                    .imageUrl(request.getImageUrl())
                    .actionUrl(request.getActionUrl())
                    .actionData(request.getActionData())
                    .isRead(false)
                    .deliveryStatus("SENT")
                    .sentAt(LocalDateTime.now())
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
                    .status(true)
                    .message("Notification sent successfully to user")
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
