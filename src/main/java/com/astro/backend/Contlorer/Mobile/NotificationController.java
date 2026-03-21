package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.ChatNotification;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.ChatNotificationRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.SendNotificationRequest;
import com.astro.backend.RequestDTO.SendNotificationByMobileRequest;
import com.astro.backend.RequestDTO.TestNotificationRequest;
import com.astro.backend.ResponseDTO.NotificationResponse;
import com.astro.backend.Services.ErrorLogService;
import com.astro.backend.Services.FcmPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final ChatNotificationRepository chatNotificationRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final FcmPushService fcmPushService;
    private final ErrorLogService errorLogService;

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
        return sendNotificationToUserId(
                request.getUserId(),
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getImageUrl(),
                request.getActionUrl(),
                request.getActionData()
        );
    }

    @PostMapping("/send-by-mobile")
    public ResponseEntity<NotificationResponse> sendNotificationByMobile(
            @Valid @RequestBody SendNotificationByMobileRequest request
    ) {
        String normalizedMobile = normalizeMobile(request.getMobileNumber());
        if (normalizedMobile.isBlank()) {
            return ResponseEntity.badRequest().body(
                    NotificationResponse.builder()
                            .status(false)
                            .message("Invalid mobile number")
                            .build()
            );
        }

        User user = userRepository.findByMobileNumber(normalizedMobile).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    NotificationResponse.builder()
                            .status(false)
                            .message("User not found for mobile number: " + normalizedMobile)
                            .build()
            );
        }

        return sendNotificationToUserId(
                user.getId(),
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getImageUrl(),
                request.getActionUrl(),
                request.getActionData()
        );
    }

    @GetMapping("/chat/pending")
    public ResponseEntity<Map<String, Object>> getPendingChatNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "120") Integer limit
    ) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid userId",
                    "items", List.of()
            ));
        }

        int safeLimit = limit == null ? 120 : limit;
        if (safeLimit <= 0) safeLimit = 120;
        if (safeLimit > 200) safeLimit = 200;

        List<ChatNotification> rows = chatNotificationRepository
                .findTop200ByUserIdAndTypeAndIsReadFalseAndIsActiveTrueOrderByCreatedAtAsc(
                        userId,
                        ChatNotification.NotificationType.SESSION
                );

        if (rows.size() > safeLimit) {
            rows = rows.subList(0, safeLimit);
        }

        List<Map<String, Object>> items = rows.stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("userId", item.getUserId());
            row.put("title", item.getTitle());
            row.put("message", item.getMessage());
            row.put("type", item.getType() == null ? null : item.getType().name());
            row.put("actionData", item.getActionData());
            row.put("createdAt", item.getCreatedAt());
            row.put("sentAt", item.getSentAt());
            return row;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", items.size(),
                "items", items
        ));
    }

    @PostMapping("/chat/ack")
    public ResponseEntity<Map<String, Object>> acknowledgeChatNotifications(
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Map<String, Object> payload = body == null ? Map.of() : body;
        Long userId = parseLong(payload.get("userId"));
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid userId"
            ));
        }

        List<Long> ids = parseLongList(payload.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "updated", 0
            ));
        }

        List<ChatNotification> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            if (id == null || id <= 0) continue;
            Optional<ChatNotification> notificationOpt = chatNotificationRepository.findByIdAndUserId(id, userId);
            if (notificationOpt.isEmpty()) continue;
            ChatNotification notification = notificationOpt.get();
            if (Boolean.TRUE.equals(notification.getIsRead())) continue;
            notification.setIsRead(true);
            notification.setReadAt(now);
            toSave.add(notification);
        }

        if (!toSave.isEmpty()) {
            chatNotificationRepository.saveAll(toSave);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "updated", toSave.size()
        ));
    }

    private ResponseEntity<NotificationResponse> sendNotificationToUserId(
            Long userId,
            String title,
            String message,
            com.astro.backend.Entity.Notification.NotificationType type,
            String imageUrl,
            String actionUrl,
            String actionData
    ) {
        try {
            // Validate user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            ChatNotification.NotificationType notificationType = type != null
                    ? ChatNotification.NotificationType.valueOf(type.name())
                    : ChatNotification.NotificationType.PROMO;

            // If user opted out from promotional notifications, ignore promo sends
            if (notificationType == ChatNotification.NotificationType.PROMO
                    && Boolean.FALSE.equals(user.getPromotionalNotificationsEnabled())) {
                return ResponseEntity.ok(
                        NotificationResponse.builder()
                                .userId(userId)
                                .type(notificationType.toString())
                                .status(false)
                                .message("User has opted out from promotional notifications")
                                .build()
                );
            }

            String fcmToken = mobileUserProfileRepository.findByUserId(userId)
                    .map(p -> p.getFcmToken() == null ? "" : p.getFcmToken().trim())
                    .orElse("");

            String deliveryStatus = "FAILED";
            String failureReason = null;
            if (fcmToken.isBlank()) {
                failureReason = "Missing FCM token";
            } else {
                FcmPushService.PushResult pushResult = fcmPushService.sendToToken(
                        fcmToken,
                        title.trim(),
                        message.trim(),
                        notificationType.name(),
                        imageUrl,
                        actionUrl,
                        actionData
                );
                if (pushResult.isSuccess()) {
                    deliveryStatus = "SENT";
                } else {
                    failureReason = pushResult.getReason();
                    if (pushResult.getRawResponse() != null && !pushResult.getRawResponse().isBlank()) {
                        failureReason = failureReason + " | " + pushResult.getRawResponse();
                    }
                    failureReason = limitFailureReason(failureReason);
                }
            }

            // Build notification object
            ChatNotification notification = ChatNotification.builder()
                    .userId(userId)
                    .title(title.trim())
                    .message(message.trim())
                    .type(notificationType)
                    .imageUrl(imageUrl)
                    .actionUrl(actionUrl)
                    .actionData(actionData)
                    .isRead(false)
                    .deliveryStatus(deliveryStatus)
                    .failureReason(failureReason)
                    .sentAt("SENT".equals(deliveryStatus) ? LocalDateTime.now() : null)
                    .build();

            // Save notification to database
            ChatNotification savedNotification = chatNotificationRepository.save(notification);

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
            Long errorId = errorLogService.log(
                    "MOBILE_NOTIFICATION",
                    "/notification/send",
                    e,
                    "{userId:" + userId + "}",
                    userId
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NotificationResponse.builder()
                            .status(false)
                            .message(errorId == null
                                    ? "Notification request failed. Please try again."
                                    : "Notification request failed. Ref: " + errorId)
                            .build());
        }
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits;
    }

    /**
     * Validate label format
     * Allowed: alphanumeric, hyphens, underscores
     */
    private boolean isValidLabel(String label) {
        return label != null && !label.isEmpty() && label.matches("^[a-zA-Z0-9_-]+$");
    }

    private String limitFailureReason(String value) {
        if (value == null) return null;
        if (value.length() <= 1500) return value;
        return value.substring(0, 1500);
    }

    private Long parseLong(Object raw) {
        if (raw == null) return null;
        try {
            return Long.parseLong(raw.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> parseLongList(Object raw) {
        if (!(raw instanceof List<?> values)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Object value : values) {
            Long parsed = parseLong(value);
            if (parsed != null && parsed > 0) {
                ids.add(parsed);
            }
        }
        return ids;
    }
}
