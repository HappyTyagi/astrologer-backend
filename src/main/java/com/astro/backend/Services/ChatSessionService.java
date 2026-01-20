package com.astro.backend.Services;


import com.astro.backend.Entity.ChatSession;
import com.astro.backend.Repositry.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository sessionRepo;
    private final WalletService walletService;

    private static final double RATE_PER_MIN = 20.0;
    private static final String CURRENCY = "INR";

    public ChatSession startSession(Long userId, Long astrologerId) {

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .astrologerId(astrologerId)
                .ratePerMin(RATE_PER_MIN)
                .currency(CURRENCY)
                .startTime(LocalDateTime.now())
                .status(ChatSession.Status.STARTED)
                .build();

        return sessionRepo.save(session);
    }

    public ChatSession endSession(ChatSession session, ChatSession.Status status) {
        session.setEndTime(LocalDateTime.now());
        session.setStatus(status);
        return sessionRepo.save(session);
    }
}

