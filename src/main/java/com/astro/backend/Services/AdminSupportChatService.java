package com.astro.backend.Services;

import com.astro.backend.Entity.AdminSupportCallSession;
import com.astro.backend.Entity.AdminSupportChatSession;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.AdminSupportCallSessionRepository;
import com.astro.backend.Repositry.AdminSupportChatSessionRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.AdminSupportCallStatusRequest;
import com.astro.backend.RequestDTO.AdminSupportMessageActivityRequest;
import com.astro.backend.RequestDTO.AdminSupportPresenceRequest;
import com.astro.backend.RequestDTO.AdminSupportSessionInitRequest;
import com.astro.backend.RequestDTO.AdminSupportStartCallRequest;
import com.astro.backend.ResponseDTO.AgoraRtmTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSupportChatService {
    public static final Long ADMIN_USER_ID = 1L;
    private static final Path UPLOAD_DIR = Path.of("uploads", "admin-support");

    private final AdminSupportChatSessionRepository sessionRepository;
    private final AdminSupportCallSessionRepository callRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final AgoraTokenService agoraTokenService;

    public User requireCurrentUser(Long currentUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalStateException("Authenticated user not found");
        }
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user does not exist"));
    }

    public AgoraRtmTokenResponse generateRtmToken(User actor) {
        return agoraTokenService.generateRtmToken(buildRtmUserId(actor));
    }

    public AdminSupportChatSession initializeSession(User actor, AdminSupportSessionInitRequest request) {
        ensureNotAdmin(actor);

        Long userId = actor.getId();
        String userName = firstNonBlank(
                safe(request.getUserName()),
                profileFor(userId).map(MobileUserProfile::getName).orElse(null),
                actor.getName(),
                "User"
        );
        String userPhone = firstNonBlank(
                safe(request.getUserPhone()),
                profileFor(userId).map(MobileUserProfile::getMobileNumber).orElse(null),
                actor.getMobileNumber()
        );
        String userAvatar = firstNonBlank(
                safe(request.getUserAvatar()),
                profileFor(userId).map(MobileUserProfile::getProfileImageUrl).orElse(null)
        );

        AdminSupportChatSession session = sessionRepository
                .findByUserIdAndAdminUserId(userId, ADMIN_USER_ID)
                .orElseGet(() -> AdminSupportChatSession.builder()
                        .chatId(buildChatId(userId))
                        .userId(userId)
                        .adminUserId(ADMIN_USER_ID)
                        .rtmChannelName(buildChannelName(userId))
                        .userRtmId(buildUserRtmId(userId))
                        .adminRtmId(buildAdminRtmId(ADMIN_USER_ID))
                        .lastMessage("Chat started")
                        .lastMessageType("text")
                        .lastMessageAt(LocalDateTime.now())
                        .lastReadByUserAt(LocalDateTime.now())
                        .build());

        session.setUserName(userName);
        session.setUserPhone(userPhone);
        if (userAvatar != null && !userAvatar.isBlank()) {
            session.setUserAvatar(userAvatar);
        }
        session.setRtmChannelName(buildChannelName(userId));
        session.setUserRtmId(buildUserRtmId(userId));
        session.setAdminRtmId(buildAdminRtmId(ADMIN_USER_ID));
        session.setIsUserOnline(true);
        session.setUserLastSeenAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public AdminSupportChatSession getSession(User actor, String chatId) {
        AdminSupportChatSession session = sessionRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
        ensureSessionAccess(actor, session);
        return session;
    }

    public List<AdminSupportChatSession> listAdminSessions(User actor) {
        ensureAdmin(actor);
        return sessionRepository.findAllByOrderByLastMessageAtDesc();
    }

    public int getAdminUnreadCount(User actor) {
        ensureAdmin(actor);
        return sessionRepository.findAllByOrderByLastMessageAtDesc()
                .stream()
                .map(AdminSupportChatSession::getAdminUnreadCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public AdminSupportChatSession registerMessageActivity(
            User actor,
            String chatId,
            AdminSupportMessageActivityRequest request
    ) {
        AdminSupportChatSession session = getSession(actor, chatId);
        String senderRole = normalizeSenderRole(actor, request.getSenderRole());
        String preview = safe(request.getPreview());
        String messageType = safe(request.getMessageType()).isBlank()
                ? "text"
                : safe(request.getMessageType()).trim().toLowerCase(Locale.ROOT);

        if ("user".equals(senderRole)) {
            session.setAdminUnreadCount(safeInt(session.getAdminUnreadCount()) + 1);
            session.setUserUnreadCount(0);
            session.setLastReadByUserAt(LocalDateTime.now());
            if (!safe(request.getUserName()).isBlank()) {
                session.setUserName(request.getUserName().trim());
            }
            if (!safe(request.getUserPhone()).isBlank()) {
                session.setUserPhone(request.getUserPhone().trim());
            }
            if (!safe(request.getUserAvatar()).isBlank()) {
                session.setUserAvatar(request.getUserAvatar().trim());
            }
        } else {
            session.setUserUnreadCount(safeInt(session.getUserUnreadCount()) + 1);
            session.setAdminUnreadCount(0);
            session.setLastReadByAdminAt(LocalDateTime.now());
        }

        session.setLastMessage(preview.isBlank() ? "Message" : preview.trim());
        session.setLastMessageType(messageType);
        session.setLastMessageAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public AdminSupportChatSession markRead(User actor, String chatId, String role) {
        AdminSupportChatSession session = getSession(actor, chatId);
        String resolvedRole = normalizeSenderRole(actor, role);
        if ("admin".equals(resolvedRole)) {
            session.setAdminUnreadCount(0);
            session.setLastReadByAdminAt(LocalDateTime.now());
        } else {
            session.setUserUnreadCount(0);
            session.setLastReadByUserAt(LocalDateTime.now());
        }
        return sessionRepository.save(session);
    }

    public AdminSupportChatSession updatePresence(
            User actor,
            String chatId,
            AdminSupportPresenceRequest request
    ) {
        AdminSupportChatSession session = getSession(actor, chatId);
        if (!session.getUserId().equals(actor.getId()) && actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only the linked user can update presence");
        }
        boolean isOnline = request.getIsOnline() == Boolean.TRUE;
        session.setIsUserOnline(isOnline);
        session.setUserLastSeenAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public AdminSupportCallSession startCall(
            User actor,
            String chatId,
            AdminSupportStartCallRequest request
    ) {
        AdminSupportChatSession session = getSession(actor, chatId);
        String callType = normalizeCallType(request.getCallType());

        Long receiverId = actor.getRole() == Role.ADMIN
                ? session.getUserId()
                : session.getAdminUserId();

        AdminSupportCallSession call = AdminSupportCallSession.builder()
                .id(UUID.randomUUID().toString())
                .chatId(chatId)
                .initiatorId(actor.getId())
                .receiverId(receiverId)
                .initiatorName(firstNonBlank(request.getInitiatorName(), actor.getName(), "User"))
                .callType(callType)
                .status("incoming")
                .build();
        return callRepository.save(call);
    }

    public AdminSupportCallSession updateCallStatus(
            User actor,
            String chatId,
            String callId,
            AdminSupportCallStatusRequest request
    ) {
        AdminSupportChatSession session = getSession(actor, chatId);
        ensureSessionAccess(actor, session);

        AdminSupportCallSession call = callRepository.findByIdAndChatId(callId, chatId)
                .orElseThrow(() -> new IllegalArgumentException("Call session not found"));
        String status = safe(request.getStatus()).isBlank()
                ? "ended"
                : request.getStatus().trim().toLowerCase(Locale.ROOT);
        call.setStatus(status);
        if ("active".equals(status)) {
            call.setAcceptedAt(LocalDateTime.now());
        }
        if ("ended".equals(status) || "rejected".equals(status) || "missed".equals(status)) {
            call.setEndedAt(LocalDateTime.now());
            call.setEndedBy(request.getEndedBy() != null ? request.getEndedBy() : actor.getId().intValue());
        }
        return callRepository.save(call);
    }

    public Optional<AdminSupportCallSession> getLatestCall(User actor, String chatId) {
        getSession(actor, chatId);
        return callRepository.findTopByChatIdOrderByCreatedAtDesc(chatId);
    }

    public Optional<AdminSupportCallSession> getCall(User actor, String chatId, String callId) {
        getSession(actor, chatId);
        return callRepository.findByIdAndChatId(callId, chatId);
    }

    public Optional<AdminSupportCallSession> getIncomingCall(User actor, Long receiverId) {
        if (receiverId == null || receiverId <= 0) {
            return Optional.empty();
        }
        if (actor.getRole() != Role.ADMIN && !actor.getId().equals(receiverId)) {
            throw new IllegalArgumentException("Access denied for incoming call lookup");
        }
        return callRepository.findTop1ByReceiverIdAndStatusOrderByCreatedAtDesc(receiverId, "incoming");
    }

    public Map<String, Object> uploadMedia(
            User actor,
            MultipartFile file,
            HttpServletRequest request
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = StringUtils.cleanPath(
                Objects.toString(file.getOriginalFilename(), "upload.bin")
        );
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0) {
            ext = originalName.substring(idx).toLowerCase(Locale.ROOT);
        }
        if (ext.isBlank()) {
            ext = ".bin";
        }

        String prefix = actor.getRole() == Role.ADMIN ? "admin" : "user";
        String safeName = prefix + "_chat_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = UPLOAD_DIR.resolve(safeName);

        try {
            Files.createDirectories(UPLOAD_DIR);
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save upload: " + e.getMessage(), e);
        }

        String baseUrl = buildBaseUrl(request);
        String url = baseUrl + "/api/admin-support/media/" + safeName;
        String contentType = safe(file.getContentType());
        if (contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String dataUrl;
        try {
            dataUrl = "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            dataUrl = null;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("url", url);
        response.put("fileName", originalName);
        response.put("storedFileName", safeName);
        response.put("size", file.getSize());
        response.put("contentType", contentType);
        response.put("dataUrl", dataUrl);
        return response;
    }

    public ResponseEntity<?> serveMedia(String fileName) {
        String cleaned = StringUtils.cleanPath(fileName);
        if (cleaned.contains("..")) {
            return ResponseEntity.badRequest().body("Invalid file path");
        }

        Path file = UPLOAD_DIR.resolve(cleaned);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(bytes));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read file");
        }
    }

    public Map<String, Object> toSessionPayload(AdminSupportChatSession session, User actor) {
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chatId", session.getChatId());
        payload.put("userId", session.getUserId());
        payload.put("userName", safe(session.getUserName()));
        payload.put("userAvatar", session.getUserAvatar());
        payload.put("userPhone", session.getUserPhone());
        payload.put("lastMessage", safe(session.getLastMessage()));
        payload.put("lastMessageType", firstNonBlank(session.getLastMessageType(), "text"));
        payload.put("lastMessageTime", session.getLastMessageAt() == null ? null : session.getLastMessageAt().toString());
        payload.put("unreadCount", isAdmin ? safeInt(session.getAdminUnreadCount()) : safeInt(session.getUserUnreadCount()));
        payload.put("adminUnreadCount", safeInt(session.getAdminUnreadCount()));
        payload.put("userUnreadCount", safeInt(session.getUserUnreadCount()));
        payload.put("isUserOnline", session.getIsUserOnline() == Boolean.TRUE);
        payload.put("rtmChannelName", session.getRtmChannelName());
        payload.put("userRtmId", session.getUserRtmId());
        payload.put("adminRtmId", session.getAdminRtmId());
        payload.put("adminUserId", session.getAdminUserId());
        payload.put("userLastSeenAt", session.getUserLastSeenAt() == null ? null : session.getUserLastSeenAt().toString());
        return payload;
    }

    public Map<String, Object> toCallPayload(AdminSupportCallSession call) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", call.getId());
        payload.put("chatId", call.getChatId());
        payload.put("initiatorId", call.getInitiatorId());
        payload.put("receiverId", call.getReceiverId());
        payload.put("initiatorName", call.getInitiatorName());
        payload.put("callType", call.getCallType());
        payload.put("status", call.getStatus());
        payload.put("createdAt", call.getCreatedAt() == null ? null : call.getCreatedAt().toString());
        payload.put("acceptedAt", call.getAcceptedAt() == null ? null : call.getAcceptedAt().toString());
        payload.put("endedAt", call.getEndedAt() == null ? null : call.getEndedAt().toString());
        payload.put("endedBy", call.getEndedBy());
        return payload;
    }

    private Optional<MobileUserProfile> profileFor(Long userId) {
        return mobileUserProfileRepository.findByUserId(userId);
    }

    private void ensureAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }

    private void ensureNotAdmin(User actor) {
        if (actor.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin cannot initialize user support chat from mobile endpoint");
        }
    }

    private void ensureSessionAccess(User actor, AdminSupportChatSession session) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (!session.getUserId().equals(actor.getId())) {
            throw new IllegalArgumentException("Access denied for this chat session");
        }
    }

    private String normalizeSenderRole(User actor, String requestedRole) {
        if (actor.getRole() == Role.ADMIN) {
            return "admin";
        }
        String normalized = safe(requestedRole).trim().toLowerCase(Locale.ROOT);
        if ("admin".equals(normalized)) {
            throw new IllegalArgumentException("User cannot act as admin");
        }
        return "user";
    }

    private String normalizeCallType(String callType) {
        String normalized = safe(callType).trim().toLowerCase(Locale.ROOT);
        return "video".equals(normalized) ? "video" : "audio";
    }

    private String buildBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                ? ""
                : ":" + request.getServerPort());
    }

    public String buildChatId(Long userId) {
        return "user_" + userId + "_admin_" + ADMIN_USER_ID;
    }

    public String buildChannelName(Long userId) {
        return "admin_support_" + userId;
    }

    public String buildRtmUserId(User actor) {
        return actor.getRole() == Role.ADMIN
                ? buildAdminRtmId(actor.getId())
                : buildUserRtmId(actor.getId());
    }

    public String buildUserRtmId(Long userId) {
        return "user_" + userId;
    }

    public String buildAdminRtmId(Long adminUserId) {
        return "admin_" + adminUserId;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
