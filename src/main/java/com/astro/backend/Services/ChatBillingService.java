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
            if (session.getStartTime() == null || session.getUserId() == null) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            long elapsedLong = Duration.between(session.getStartTime(), now).toMinutes();
            int elapsedMinutes = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, elapsedLong));
            int processedMinutes = Math.max(0, session.getTotalMinutes());

            if (elapsedMinutes <= processedMinutes) {
                continue;
            }

            int freeMinutes = session.getFreeMinutesAllowed() == null
                    ? 0
                    : Math.max(0, session.getFreeMinutesAllowed());
            double ratePerMin = Math.max(0.0, session.getRatePerMin());

            while (processedMinutes < elapsedMinutes) {
                int nextMinute = processedMinutes + 1;

                if (nextMinute > freeMinutes && ratePerMin > 0.0) {
                    String ref = "CHAT-" + session.getId() + "-" + nextMinute;
                    boolean debited = walletService.debit(
                            session.getUserId(),
                            ratePerMin,
                            ref,
                            "Chat billing for session " + session.getId() + " minute " + nextMinute
                    );

                    if (!debited) {
                        session.setStatus(ChatSession.Status.AUTO_ENDED);
                        session.setEndTime(now);
                        break;
                    }
                }

                processedMinutes = nextMinute;
            }

            session.setTotalMinutes(processedMinutes);
            sessionRepo.save(session);
        }
    }
}
