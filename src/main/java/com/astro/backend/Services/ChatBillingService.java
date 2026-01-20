package com.astro.backend.Services;


import com.astro.backend.Entity.ChatSession;
import com.astro.backend.Repositry.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatBillingService {

    private final ChatSessionRepository sessionRepo;
    private final WalletService walletService;

    @Scheduled(fixedRate = 60000)
    public void billActiveSessions() {
        List<ChatSession> activeSessions = sessionRepo.findAll()
                .stream()
                .filter(s -> s.getStatus() == ChatSession.Status.STARTED)
                .toList();

        for (ChatSession session : activeSessions) {
            long minutes = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
            int billedMinutes = (int) minutes;

            if (billedMinutes > session.getTotalMinutes()) {

                boolean debited = walletService.debit(
                        session.getUserId(),
                        session.getRatePerMin(),
                        "CHAT_BILLING",
                        "Chat billing for session " + session.getId()
                );

                if (!debited) {
                    session.setStatus(ChatSession.Status.AUTO_ENDED);
                    session.setEndTime(LocalDateTime.now());
                    sessionRepo.save(session);
                    continue;
                }

                session.setTotalMinutes(billedMinutes);
                sessionRepo.save(session);
            }
        }
    }
}

