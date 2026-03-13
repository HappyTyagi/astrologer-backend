package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.ChatSession;
import com.astro.backend.Repositry.ChatMessageRepository;
import com.astro.backend.Repositry.ChatSessionRepository;
import com.astro.backend.Services.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private static final Long ADMIN_ASTROLOGER_ID = 1L;

    private final ChatSessionService sessionService;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;

    @PostMapping("/start")
    public ResponseEntity<ChatSession> start(
            @RequestParam Long userId,
            @RequestParam(required = false) Long astrologerId
    ) {
        if (astrologerId != null && !ADMIN_ASTROLOGER_ID.equals(astrologerId)) {
            throw new IllegalArgumentException("Users can chat only with admin");
        }
        try {
            return ResponseEntity.ok(sessionService.startSession(userId, ADMIN_ASTROLOGER_ID));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/stop/{sessionId}")
    public ChatSession stop(@PathVariable Long sessionId) {
        ChatSession s = sessionRepo.findById(sessionId).orElseThrow();
        return sessionService.endSession(s, ChatSession.Status.ENDED);
    }

    @GetMapping("/history/{sessionId}")
    public Object history(@PathVariable Long sessionId) {
        return messageRepo.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}
