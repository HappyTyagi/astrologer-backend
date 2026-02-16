package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.AdminNotificationDispatch;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.WebNotification;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.AdminNotificationDispatchRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.WebNotificationRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.Services.ErrorLogService;
import com.astro.backend.Services.FcmPushService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/web/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private static final Path UPLOAD_DIR = Path.of("uploads", "notifications");

    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final WebNotificationRepository webNotificationRepository;
    private final AdminNotificationDispatchRepository dispatchRepository;
    private final FcmPushService fcmPushService;
    private final ErrorLogService errorLogService;

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        List<Map<String, Object>> rows = dispatchRepository.findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDispatchRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        User sender = getCurrentAdmin();
        if (sender == null || sender.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", false,
                    "message", "Access denied"
            ));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", "Image file is required"
            ));
        }

        String originalName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "upload.bin"));
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0) ext = originalName.substring(idx).toLowerCase(Locale.ROOT);
        if (ext.isBlank()) ext = ".bin";

        String safeName = "notification_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = UPLOAD_DIR.resolve(safeName);

        try {
            Files.createDirectories(UPLOAD_DIR);
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", false,
                    "message", "Failed to save image: " + e.getMessage()
            ));
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName() +
                ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        String imageUrl = baseUrl + "/api/web/notifications/image/" + safeName;

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
        String imageBase64;
        try {
            imageBase64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            imageBase64 = null;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", true);
        resp.put("url", imageUrl);
        resp.put("fileName", safeName);
        resp.put("imageBase64", imageBase64);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<?> serveImage(@PathVariable String fileName) {
        String cleaned = StringUtils.cleanPath(fileName);
        if (cleaned.contains("..")) {
            return ResponseEntity.badRequest().body("Invalid file path");
        }
        Path file = UPLOAD_DIR.resolve(cleaned);
        if (!Files.exists(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null || contentType.isBlank()) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(bytes));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read image");
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody SendAdminNotificationRequest request) {
        User sender = getCurrentAdmin();
        if (sender == null || sender.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", false,
                    "message", "Access denied"
            ));
        }
        try {
            String title = safe(request.getTitle());
            String message = safe(request.getMessage());
            String audienceType = safe(request.getAudienceType()).toUpperCase(Locale.ROOT);
            String typeRaw = safe(request.getType()).toUpperCase(Locale.ROOT);

            if (title.isBlank() || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "Title and message are required"
                ));
            }

            if (!"BROADCAST".equals(audienceType) && !"INDIVIDUAL".equals(audienceType)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "audienceType must be BROADCAST or INDIVIDUAL"
                ));
            }

            WebNotification.NotificationType type = parseType(typeRaw);

            List<MobileUserProfile> targets;
            if ("INDIVIDUAL".equals(audienceType)) {
                if (request.getTargetUserId() == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", false,
                            "message", "targetUserId is required for INDIVIDUAL send"
                    ));
                }
                Optional<MobileUserProfile> target = mobileUserProfileRepository.findByUserId(request.getTargetUserId());
                if (target.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", false,
                            "message", "Mobile profile not found for targetUserId"
                    ));
                }
                targets = List.of(target.get());
            } else {
                targets = mobileUserProfileRepository.findAll();
            }

            if (targets.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", false,
                        "message", "No target users found"
                ));
            }

            Set<Long> targetUserIds = targets.stream().map(MobileUserProfile::getUserId).collect(Collectors.toSet());
            Map<Long, User> userMap = userRepository.findAllById(targetUserIds)
                    .stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            List<WebNotification> notifications = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;

            String resolvedImage = blankToNull(request.getImageBase64());
            if (resolvedImage == null) resolvedImage = blankToNull(request.getImageUrl());
            String pushImageUrl = blankToNull(request.getImageUrl());

            for (MobileUserProfile profile : targets) {
                User targetUser = userMap.get(profile.getUserId());

                if (type == WebNotification.NotificationType.PROMO
                        && targetUser != null
                        && Boolean.FALSE.equals(targetUser.getPromotionalNotificationsEnabled())) {
                    failedCount++;
                    continue;
                }

                WebNotification notification = WebNotification.builder()
                        .userId(profile.getUserId())
                        .senderUserId(sender.getId())
                        .audienceType(audienceType)
                        .title(title)
                        .message(message)
                        .type(type)
                        .imageUrl(resolvedImage)
                        .actionUrl(blankToNull(request.getActionUrl()))
                        .actionData(buildActionData(sender, audienceType, profile))
                        .isRead(false)
                        .deliveryStatus("PENDING")
                        .build();

                String token = safe(profile.getFcmToken());
                if (token.isBlank()) {
                    notification.setDeliveryStatus("FAILED");
                    notification.setFailureReason("Missing FCM token");
                    failedCount++;
                } else {
                    FcmPushService.PushResult pushResult = fcmPushService.sendToToken(
                            token,
                            title,
                            message,
                            type.name(),
                            pushImageUrl,
                            blankToNull(request.getActionUrl()),
                            notification.getActionData()
                    );
                    if (pushResult.isSuccess()) {
                        notification.setDeliveryStatus("SENT");
                        notification.setSentAt(LocalDateTime.now());
                        successCount++;
                    } else {
                        notification.setDeliveryStatus("FAILED");
                        String failure = pushResult.getReason();
                        if (pushResult.getRawResponse() != null && !pushResult.getRawResponse().isBlank()) {
                            failure = failure + " | " + pushResult.getRawResponse();
                        }
                        notification.setFailureReason(limitFailureReason(failure));
                        failedCount++;
                    }
                }
                notifications.add(notification);
            }

            if (!notifications.isEmpty()) {
                webNotificationRepository.saveAll(notifications);
            }

            MobileUserProfile firstTarget = targets.get(0);
            AdminNotificationDispatch dispatch = AdminNotificationDispatch.builder()
                    .senderUserId(sender.getId())
                    .senderEmail(safe(sender.getEmail()))
                    .audienceType(audienceType)
                    .targetUserId("INDIVIDUAL".equals(audienceType) ? firstTarget.getUserId() : null)
                    .targetUserName("INDIVIDUAL".equals(audienceType) ? firstTarget.getName() : null)
                    .targetMobileNumber("INDIVIDUAL".equals(audienceType) ? firstTarget.getMobileNumber() : null)
                    .title(title)
                    .message(message)
                    .notificationType(type.name())
                    .requestedCount(targets.size())
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .status(failedCount > 0 ? "PARTIAL" : "SENT")
                    .notes("Push dispatch attempted via FCM. DB entries reflect per-user deliveryStatus.")
                    .build();

            AdminNotificationDispatch savedDispatch = dispatchRepository.save(dispatch);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", true);
            response.put("message", "Notification processed");
            response.put("dispatch", toDispatchRow(savedDispatch));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Long errorId = errorLogService.log(
                    "ADMIN_NOTIFICATION",
                    "/api/web/notifications/send",
                    e,
                    "{audienceType:" + safe(request.getAudienceType()) + ",targetUserId:" + request.getTargetUserId() + "}",
                    sender.getId()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", false,
                    "message", errorId == null
                            ? "Notification request failed. Please try again."
                            : "Notification request failed. Ref: " + errorId
            ));
        }
    }

    private Map<String, Object> toDispatchRow(AdminNotificationDispatch d) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", d.getId());
        row.put("senderUserId", d.getSenderUserId());
        row.put("senderEmail", d.getSenderEmail());
        row.put("audienceType", d.getAudienceType());
        row.put("targetUserId", d.getTargetUserId());
        row.put("targetUserName", d.getTargetUserName());
        row.put("targetMobileNumber", d.getTargetMobileNumber());
        row.put("title", d.getTitle());
        row.put("message", d.getMessage());
        row.put("notificationType", d.getNotificationType());
        row.put("requestedCount", d.getRequestedCount());
        row.put("successCount", d.getSuccessCount());
        row.put("failedCount", d.getFailedCount());
        row.put("status", d.getStatus());
        row.put("notes", d.getNotes());
        row.put("createdAt", d.getCreatedAt());
        return row;
    }

    private User getCurrentAdmin() {
        String principalEmail = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return userRepository.findByEmail(principalEmail).orElse(null);
    }

    private WebNotification.NotificationType parseType(String raw) {
        if (raw == null || raw.isBlank()) return WebNotification.NotificationType.PROMO;
        try {
            return WebNotification.NotificationType.valueOf(raw);
        } catch (Exception ignored) {
            return WebNotification.NotificationType.PROMO;
        }
    }

    private String buildActionData(User sender, String audienceType, MobileUserProfile recipient) {
        return String.format(Locale.ROOT,
                "{\"source\":\"ADMIN_PANEL\",\"senderId\":%d,\"senderEmail\":\"%s\",\"audience\":\"%s\",\"recipientUserId\":%d}",
                sender.getId(), safe(sender.getEmail()), audienceType, recipient.getUserId());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String v = safe(value);
        return v.isBlank() ? null : v;
    }

    private String limitFailureReason(String value) {
        if (value == null) return null;
        if (value.length() <= 1500) return value;
        return value.substring(0, 1500);
    }

    @Data
    private static class SendAdminNotificationRequest {
        private String title;
        private String message;
        private String audienceType; // BROADCAST / INDIVIDUAL
        private Long targetUserId;   // required for INDIVIDUAL
        private String type;         // NotificationType enum value
        private String imageUrl;
        private String imageBase64;
        private String actionUrl;
    }
}
