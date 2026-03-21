package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Auth.JwtAuthFilter;
import com.astro.backend.Entity.AdminSupportCallSession;
import com.astro.backend.Entity.AdminSupportChatSession;
import com.astro.backend.Entity.User;
import com.astro.backend.RequestDTO.AdminSupportCallStatusRequest;
import com.astro.backend.RequestDTO.AdminSupportMessageActivityRequest;
import com.astro.backend.RequestDTO.AdminSupportPresenceRequest;
import com.astro.backend.RequestDTO.AdminSupportReadRequest;
import com.astro.backend.RequestDTO.AdminSupportSessionInitRequest;
import com.astro.backend.RequestDTO.AdminSupportStartCallRequest;
import com.astro.backend.ResponseDTO.AgoraRtmTokenResponse;
import com.astro.backend.Services.AdminSupportChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin-support")
@RequiredArgsConstructor
public class AdminSupportChatController {

    private final AdminSupportChatService adminSupportChatService;

    @GetMapping("/rtm/token")
    public ResponseEntity<?> getRtmToken(HttpServletRequest request) {
        User actor = currentUser(request);
        AgoraRtmTokenResponse response = adminSupportChatService.generateRtmToken(actor);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/init")
    public ResponseEntity<?> initializeSession(
            @RequestBody(required = false) AdminSupportSessionInitRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportChatSession session = adminSupportChatService.initializeSession(
                actor,
                body == null ? new AdminSupportSessionInitRequest() : body
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @GetMapping("/sessions/{chatId}")
    public ResponseEntity<?> getSession(
            @PathVariable String chatId,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportChatSession session = adminSupportChatService.getSession(actor, chatId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @GetMapping("/sessions/admin")
    public ResponseEntity<?> listAdminSessions(HttpServletRequest request) {
        User actor = currentUser(request);
        List<Map<String, Object>> rows = adminSupportChatService.listAdminSessions(actor)
                .stream()
                .map(session -> adminSupportChatService.toSessionPayload(session, actor))
                .toList();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sessions", rows,
                "unreadCount", adminSupportChatService.getAdminUnreadCount(actor)
        ));
    }

    @PostMapping("/sessions/{chatId}/message-activity")
    public ResponseEntity<?> registerMessageActivity(
            @PathVariable String chatId,
            @RequestBody AdminSupportMessageActivityRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportChatSession session = adminSupportChatService.registerMessageActivity(actor, chatId, body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @PostMapping("/sessions/{chatId}/read")
    public ResponseEntity<?> markRead(
            @PathVariable String chatId,
            @RequestBody(required = false) AdminSupportReadRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportChatSession session = adminSupportChatService.markRead(
                actor,
                chatId,
                body == null ? null : body.getRole()
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @PostMapping("/sessions/{chatId}/clear-user")
    public ResponseEntity<?> clearUserChat(
            @PathVariable String chatId,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportChatSession session = adminSupportChatService.clearUserChat(actor, chatId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @PostMapping("/sessions/{chatId}/presence")
    public ResponseEntity<?> updatePresence(
            @PathVariable String chatId,
            @RequestBody(required = false) AdminSupportPresenceRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportPresenceRequest payload = body == null ? new AdminSupportPresenceRequest() : body;
        AdminSupportChatSession session = adminSupportChatService.updatePresence(actor, chatId, payload);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "session", adminSupportChatService.toSessionPayload(session, actor)
        ));
    }

    @PostMapping("/sessions/{chatId}/calls")
    public ResponseEntity<?> startCall(
            @PathVariable String chatId,
            @RequestBody AdminSupportStartCallRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportCallSession call = adminSupportChatService.startCall(actor, chatId, body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "call", adminSupportChatService.toCallPayload(call)
        ));
    }

    @PostMapping("/sessions/{chatId}/calls/{callId}/status")
    public ResponseEntity<?> updateCallStatus(
            @PathVariable String chatId,
            @PathVariable String callId,
            @RequestBody AdminSupportCallStatusRequest body,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        AdminSupportCallSession call = adminSupportChatService.updateCallStatus(actor, chatId, callId, body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "call", adminSupportChatService.toCallPayload(call)
        ));
    }

    @GetMapping("/sessions/{chatId}/calls/latest")
    public ResponseEntity<?> getLatestCall(
            @PathVariable String chatId,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        Optional<AdminSupportCallSession> call = adminSupportChatService.getLatestCall(actor, chatId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("call", call.map(adminSupportChatService::toCallPayload).orElse(null));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{chatId}/calls/{callId}")
    public ResponseEntity<?> getCall(
            @PathVariable String chatId,
            @PathVariable String callId,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        Optional<AdminSupportCallSession> call = adminSupportChatService.getCall(actor, chatId, callId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("call", call.map(adminSupportChatService::toCallPayload).orElse(null));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calls/incoming")
    public ResponseEntity<?> getIncomingCall(
            @RequestParam(required = false) Long receiverUserId,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        Long lookupUserId = receiverUserId != null ? receiverUserId : actor.getId();
        Optional<AdminSupportCallSession> call = adminSupportChatService.getIncomingCall(actor, lookupUserId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("call", call.map(adminSupportChatService::toCallPayload).orElse(null));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/media/upload")
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        User actor = currentUser(request);
        return ResponseEntity.ok(adminSupportChatService.uploadMedia(actor, file, request));
    }

    @GetMapping("/media/{fileName}")
    public ResponseEntity<?> serveMedia(@PathVariable String fileName) {
        return adminSupportChatService.serveMedia(fileName);
    }

    private User currentUser(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthFilter.AUTH_USER_ID_ATTR);
        Long currentUserId = null;
        if (value instanceof Long longValue) {
            currentUserId = longValue;
        } else if (value instanceof Integer intValue) {
            currentUserId = intValue.longValue();
        } else if (value != null) {
            currentUserId = Long.parseLong(String.valueOf(value));
        }
        return adminSupportChatService.requireCurrentUser(currentUserId);
    }
}
