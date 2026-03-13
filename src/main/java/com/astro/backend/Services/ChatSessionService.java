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
    private final ChatPricingConfigService chatPricingConfigService;
    private static final String CURRENCY = "INR";

    public ChatSession startSession(Long userId, Long astrologerId) {
        ChatPricingConfigService.ChatPricingConfig pricing =
                chatPricingConfigService.getEffectiveConfigForUser(userId);

        if (!pricing.isChatAllowed()) {
            throw new RuntimeException("Chat is currently disabled for your account. Please contact support.");
        }

        double chatRatePerMin = Math.max(0.0, pricing.getChatRatePerMin());
        double minimumRequiredBalance = pricing.getMinimumRequiredBalance();

        if (minimumRequiredBalance > 0.0) {
            double walletBalance = walletService.getWallet(userId).getBalance();
            if (walletBalance + 0.000001 < minimumRequiredBalance) {
                throw new RuntimeException(
                        "Insufficient wallet balance. Please recharge wallet before starting chat."
                );
            }
        }

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .astrologerId(astrologerId)
                .ratePerMin(chatRatePerMin)
                .currency(CURRENCY)
                .freeMinutesAllowed(Math.max(0, pricing.getFreeMinutes()))
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
