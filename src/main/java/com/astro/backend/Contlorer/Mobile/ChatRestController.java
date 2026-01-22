package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.ChatSession;
import com.astro.backend.Repositry.ChatMessageRepository;
import com.astro.backend.Repositry.ChatSessionRepository;
import com.astro.backend.Services.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatSessionService sessionService;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;

    @PostMapping("/start")
    public ChatSession start(@RequestParam Long userId, @RequestParam Long astrologerId) {
        return sessionService.startSession(userId, astrologerId);
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
