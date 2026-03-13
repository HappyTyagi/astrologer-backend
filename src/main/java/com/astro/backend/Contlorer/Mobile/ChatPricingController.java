package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Services.ChatPricingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController("mobileChatPricingController")
@RequestMapping("/api/mobile/chat")
@RequiredArgsConstructor
public class ChatPricingController {

    private static final String CURRENCY = "INR";

    private final ChatPricingConfigService chatPricingConfigService;

    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing(
            @RequestParam(required = false) Long userId
    ) {
        ChatPricingConfigService.ChatPricingConfig cfg =
                userId != null && userId > 0
                        ? chatPricingConfigService.getEffectiveConfigForUser(userId)
                        : chatPricingConfigService.getOrCreateConfig();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("currency", CURRENCY);
        body.put("userId", userId);
        body.put("chatRatePerMin", cfg.getChatRatePerMin());
        body.put("freeMinutes", cfg.getFreeMinutes());
        body.put("chatAllowed", cfg.isChatAllowed());
        body.put("minimumRequiredBalance", cfg.getMinimumRequiredBalance());
        body.put("userOverrideApplied", cfg.isUserOverrideApplied());
        return ResponseEntity.ok(body);
    }
}
