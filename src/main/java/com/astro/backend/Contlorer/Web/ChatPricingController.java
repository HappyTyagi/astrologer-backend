package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.ChatPricingUpdateRequest;
import com.astro.backend.RequestDTO.UserChatPricingUpdateRequest;
import com.astro.backend.Services.ChatPricingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController("webChatPricingController")
@RequestMapping("/api/web/chat-pricing")
@RequiredArgsConstructor
public class ChatPricingController {

    private static final String CURRENCY = "INR";

    private final ChatPricingConfigService chatPricingConfigService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPricing() {
        return ResponseEntity.ok(
                buildGlobalResponse(
                        chatPricingConfigService.getOrCreateConfig(),
                        "Chat pricing fetched successfully"
                )
        );
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePricing(
            @Valid @RequestBody ChatPricingUpdateRequest request
    ) {
        try {
            ChatPricingConfigService.ChatPricingConfig saved = chatPricingConfigService.saveConfig(
                    request.getChatRatePerMin(),
                    request.getFreeMinutes(),
                    Boolean.TRUE.equals(request.getChatAllowed())
            );
            return ResponseEntity.ok(buildGlobalResponse(saved, "Chat pricing updated successfully"));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Failed to update chat pricing", e);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserPricing(@PathVariable Long userId) {
        try {
            validateTargetUser(userId);
            ChatPricingConfigService.ChatPricingConfig cfg =
                    chatPricingConfigService.getEffectiveConfigForUser(userId);
            return ResponseEntity.ok(buildUserResponse(cfg, "User chat pricing fetched successfully", userId));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Failed to fetch user chat pricing", e);
        }
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserPricing(
            @PathVariable Long userId,
            @Valid @RequestBody UserChatPricingUpdateRequest request
    ) {
        try {
            validateTargetUser(userId);
            ChatPricingConfigService.ChatPricingConfig saved =
                    chatPricingConfigService.saveUserOverride(
                            userId,
                            request.getChatRatePerMin(),
                            request.getFreeMinutes(),
                            Boolean.TRUE.equals(request.getChatAllowed())
                    );
            return ResponseEntity.ok(
                    buildUserResponse(saved, "User chat pricing updated successfully", userId)
            );
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Failed to update user chat pricing", e);
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> clearUserPricing(@PathVariable Long userId) {
        try {
            validateTargetUser(userId);
            ChatPricingConfigService.ChatPricingConfig cfg =
                    chatPricingConfigService.clearUserOverride(userId);
            return ResponseEntity.ok(
                    buildUserResponse(
                            cfg,
                            "User specific chat pricing cleared successfully",
                            userId
                    )
            );
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Failed to clear user chat pricing", e);
        }
    }

    private void validateTargetUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for userId: " + userId));
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ASTROLOGER) {
            throw new RuntimeException("Chat pricing override is allowed only for app users");
        }
    }

    private Map<String, Object> buildGlobalResponse(
            ChatPricingConfigService.ChatPricingConfig cfg,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("scope", "GLOBAL");
        body.put("currency", CURRENCY);
        body.put("configKey", ChatPricingConfigService.KEY_CHAT_RATE);
        body.put("configValue", chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.KEY_CHAT_RATE));
        body.put("freeMinutesConfigKey", ChatPricingConfigService.KEY_CHAT_FREE_MINUTES);
        body.put(
                "freeMinutesConfigValue",
                chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.KEY_CHAT_FREE_MINUTES)
        );
        body.put("chatAllowedConfigKey", ChatPricingConfigService.KEY_CHAT_ALLOWED);
        body.put(
                "chatAllowedConfigValue",
                chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.KEY_CHAT_ALLOWED)
        );
        body.put("chatRatePerMin", cfg.getChatRatePerMin());
        body.put("freeMinutes", cfg.getFreeMinutes());
        body.put("chatAllowed", cfg.isChatAllowed());
        body.put("minimumRequiredBalance", cfg.getMinimumRequiredBalance());
        body.put("userOverrideApplied", false);
        return body;
    }

    private Map<String, Object> buildUserResponse(
            ChatPricingConfigService.ChatPricingConfig cfg,
            String message,
            Long userId
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("scope", "USER");
        body.put("currency", CURRENCY);
        body.put("userId", userId);
        body.put("chatRatePerMin", cfg.getChatRatePerMin());
        body.put("freeMinutes", cfg.getFreeMinutes());
        body.put("chatAllowed", cfg.isChatAllowed());
        body.put("minimumRequiredBalance", cfg.getMinimumRequiredBalance());
        body.put("userOverrideApplied", cfg.isUserOverrideApplied());
        body.put("userRateConfigKey", ChatPricingConfigService.userRateKey(userId));
        body.put(
                "userRateConfigValue",
                chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.userRateKey(userId))
        );
        body.put("userFreeMinutesConfigKey", ChatPricingConfigService.userFreeMinutesKey(userId));
        body.put(
                "userFreeMinutesConfigValue",
                chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.userFreeMinutesKey(userId))
        );
        body.put("userChatAllowedConfigKey", ChatPricingConfigService.userAllowedKey(userId));
        body.put(
                "userChatAllowedConfigValue",
                chatPricingConfigService.getRawConfigValue(ChatPricingConfigService.userAllowedKey(userId))
        );

        ChatPricingConfigService.ChatPricingConfig global = chatPricingConfigService.getOrCreateConfig();
        body.put("globalChatRatePerMin", global.getChatRatePerMin());
        body.put("globalFreeMinutes", global.getFreeMinutes());
        body.put("globalChatAllowed", global.isChatAllowed());
        return body;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> serverError(String prefix, Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", prefix + ": " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
