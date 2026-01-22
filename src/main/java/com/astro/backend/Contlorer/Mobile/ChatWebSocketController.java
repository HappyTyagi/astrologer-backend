package com.astro.backend.Contlorer.Mobile;


import com.astro.backend.Entity.ChatMessage;
import com.astro.backend.Repositry.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageRepository messageRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send/{sessionId}")
    public void sendMessage(@DestinationVariable Long sessionId, @Payload ChatMessage msg) {
        msg.setTimestamp(LocalDateTime.now());
        msg.setSessionId(sessionId);
        messageRepo.save(msg);

        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, msg);
    }
}
