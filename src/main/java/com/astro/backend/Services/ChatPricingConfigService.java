package com.astro.backend.Services;

import com.astro.backend.Entity.AppConfig;
import com.astro.backend.Repositry.AppConfigRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatPricingConfigService {

    public static final String CATEGORY = "CHAT_PRICING";
    public static final String KEY_CHAT_RATE = "chat.rate_per_min";
    public static final String KEY_CHAT_FREE_MINUTES = "chat.free_minutes";
    public static final String KEY_CHAT_ALLOWED = "chat.allowed";

    private static final String USER_KEY_PREFIX = "chat.user.";
    private static final String USER_KEY_RATE_SUFFIX = ".rate_per_min";
    private static final String USER_KEY_FREE_MINUTES_SUFFIX = ".free_minutes";
    private static final String USER_KEY_ALLOWED_SUFFIX = ".allowed";

    private static final double DEFAULT_CHAT_RATE = 20.0;
    private static final int DEFAULT_FREE_MINUTES = 0;
    private static final boolean DEFAULT_CHAT_ALLOWED = true;

    private final AppConfigRepository appConfigRepository;

    @Getter
    @Builder
    public static class ChatPricingConfig {
        private Long userId;
        private double chatRatePerMin;
        private int freeMinutes;
        private boolean chatAllowed;
        private boolean userOverrideApplied;

        public double getMinimumRequiredBalance() {
            if (!chatAllowed || chatRatePerMin <= 0.0 || freeMinutes > 0) {
                return 0.0;
            }
            return chatRatePerMin;
        }
    }

    @Transactional
    public ChatPricingConfig getOrCreateConfig() {
        return ChatPricingConfig.builder()
                .chatRatePerMin(
                        readDouble(
                                KEY_CHAT_RATE,
                                DEFAULT_CHAT_RATE,
                                "Chat charge in INR per minute"
                        )
                )
                .freeMinutes(
                        readInt(
                                KEY_CHAT_FREE_MINUTES,
                                DEFAULT_FREE_MINUTES,
                                "Chat free minutes allowed"
                        )
                )
                .chatAllowed(
                        readBoolean(
                                KEY_CHAT_ALLOWED,
                                DEFAULT_CHAT_ALLOWED,
                                "Allow users to start chat"
                        )
                )
                .userOverrideApplied(false)
                .build();
    }

    @Transactional
    public ChatPricingConfig getEffectiveConfigForUser(Long userId) {
        ChatPricingConfig global = getOrCreateConfig();
        if (userId == null || userId <= 0) {
            return global;
        }

        Optional<Double> userRate = readOptionalDouble(userRateKey(userId));
        Optional<Integer> userFreeMinutes = readOptionalInt(userFreeMinutesKey(userId));
        Optional<Boolean> userChatAllowed = readOptionalBoolean(userAllowedKey(userId));
        boolean overrideApplied =
                userRate.isPresent() || userFreeMinutes.isPresent() || userChatAllowed.isPresent();

        return ChatPricingConfig.builder()
                .userId(userId)
                .chatRatePerMin(userRate.orElse(global.getChatRatePerMin()))
                .freeMinutes(userFreeMinutes.orElse(global.getFreeMinutes()))
                .chatAllowed(userChatAllowed.orElse(global.isChatAllowed()))
                .userOverrideApplied(overrideApplied)
                .build();
    }

    @Transactional
    public ChatPricingConfig saveConfig(double chatRatePerMin, int freeMinutes, boolean chatAllowed) {
        validateInput(chatRatePerMin, freeMinutes);
        upsertDouble(KEY_CHAT_RATE, chatRatePerMin, "Chat charge in INR per minute");
        upsertInt(KEY_CHAT_FREE_MINUTES, freeMinutes, "Chat free minutes allowed");
        upsertBoolean(KEY_CHAT_ALLOWED, chatAllowed, "Allow users to start chat");
        return getOrCreateConfig();
    }

    @Transactional
    public ChatPricingConfig saveUserOverride(
            Long userId,
            double chatRatePerMin,
            int freeMinutes,
            boolean chatAllowed
    ) {
        Long safeUserId = sanitizeUserId(userId);
        validateInput(chatRatePerMin, freeMinutes);
        upsertDouble(
                userRateKey(safeUserId),
                chatRatePerMin,
                "User specific chat charge in INR per minute"
        );
        upsertInt(
                userFreeMinutesKey(safeUserId),
                freeMinutes,
                "User specific chat free minutes"
        );
        upsertBoolean(
                userAllowedKey(safeUserId),
                chatAllowed,
                "Allow chat for this user"
        );
        return getEffectiveConfigForUser(safeUserId);
    }

    @Transactional
    public ChatPricingConfig clearUserOverride(Long userId) {
        Long safeUserId = sanitizeUserId(userId);
        deleteIfPresent(userRateKey(safeUserId));
        deleteIfPresent(userFreeMinutesKey(safeUserId));
        deleteIfPresent(userAllowedKey(safeUserId));
        return getEffectiveConfigForUser(safeUserId);
    }

    @Transactional(readOnly = true)
    public String getRawConfigValue(String key) {
        return appConfigRepository.findByConfigKey(key)
                .map(AppConfig::getConfigValue)
                .orElse(null);
    }

    public static String userRateKey(Long userId) {
        return USER_KEY_PREFIX + userId + USER_KEY_RATE_SUFFIX;
    }

    public static String userFreeMinutesKey(Long userId) {
        return USER_KEY_PREFIX + userId + USER_KEY_FREE_MINUTES_SUFFIX;
    }

    public static String userAllowedKey(Long userId) {
        return USER_KEY_PREFIX + userId + USER_KEY_ALLOWED_SUFFIX;
    }

    private Long sanitizeUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        return userId;
    }

    private void validateInput(double chatRatePerMin, int freeMinutes) {
        if (chatRatePerMin < 0) {
            throw new RuntimeException("chatRatePerMin cannot be negative");
        }
        if (freeMinutes < 0) {
            throw new RuntimeException("freeMinutes cannot be negative");
        }
    }

    private double readDouble(String key, double defaultValue, String description) {
        AppConfig cfg = upsertIfMissing(
                key,
                String.valueOf(defaultValue),
                AppConfig.ConfigType.FLOAT,
                description
        );
        try {
            return Math.max(
                    0.0,
                    Double.parseDouble(String.valueOf(cfg.getConfigValue()).trim())
            );
        } catch (Exception ignored) {
            cfg.setConfigValue(String.valueOf(defaultValue));
            cfg.setUpdatedAt(LocalDateTime.now());
            appConfigRepository.save(cfg);
            return defaultValue;
        }
    }

    private int readInt(String key, int defaultValue, String description) {
        AppConfig cfg = upsertIfMissing(
                key,
                String.valueOf(defaultValue),
                AppConfig.ConfigType.INTEGER,
                description
        );
        try {
            return Math.max(
                    0,
                    Integer.parseInt(String.valueOf(cfg.getConfigValue()).trim())
            );
        } catch (Exception ignored) {
            cfg.setConfigValue(String.valueOf(defaultValue));
            cfg.setUpdatedAt(LocalDateTime.now());
            appConfigRepository.save(cfg);
            return defaultValue;
        }
    }

    private boolean readBoolean(String key, boolean defaultValue, String description) {
        AppConfig cfg = upsertIfMissing(
                key,
                String.valueOf(defaultValue),
                AppConfig.ConfigType.BOOLEAN,
                description
        );
        return parseBoolean(cfg.getConfigValue()).orElseGet(() -> {
            cfg.setConfigValue(String.valueOf(defaultValue));
            cfg.setUpdatedAt(LocalDateTime.now());
            appConfigRepository.save(cfg);
            return defaultValue;
        });
    }

    private Optional<Double> readOptionalDouble(String key) {
        return appConfigRepository.findByConfigKey(key)
                .map(AppConfig::getConfigValue)
                .flatMap(this::parseDouble);
    }

    private Optional<Integer> readOptionalInt(String key) {
        return appConfigRepository.findByConfigKey(key)
                .map(AppConfig::getConfigValue)
                .flatMap(this::parseInt);
    }

    private Optional<Boolean> readOptionalBoolean(String key) {
        return appConfigRepository.findByConfigKey(key)
                .map(AppConfig::getConfigValue)
                .flatMap(this::parseBoolean);
    }

    private Optional<Double> parseDouble(String rawValue) {
        try {
            return Optional.of(
                    Math.max(0.0, Double.parseDouble(String.valueOf(rawValue).trim()))
            );
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Integer> parseInt(String rawValue) {
        try {
            return Optional.of(
                    Math.max(0, Integer.parseInt(String.valueOf(rawValue).trim()))
            );
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Boolean> parseBoolean(String rawValue) {
        if (rawValue == null) return Optional.empty();
        String value = rawValue.trim();
        if ("1".equals(value)) return Optional.of(true);
        if ("0".equals(value)) return Optional.of(false);
        if ("true".equalsIgnoreCase(value)) return Optional.of(true);
        if ("false".equalsIgnoreCase(value)) return Optional.of(false);
        return Optional.empty();
    }

    private void upsertDouble(String key, double value, String description) {
        upsertValue(
                key,
                String.valueOf(Math.max(0.0, value)),
                AppConfig.ConfigType.FLOAT,
                description
        );
    }

    private void upsertInt(String key, int value, String description) {
        upsertValue(
                key,
                String.valueOf(Math.max(0, value)),
                AppConfig.ConfigType.INTEGER,
                description
        );
    }

    private void upsertBoolean(String key, boolean value, String description) {
        upsertValue(
                key,
                String.valueOf(value),
                AppConfig.ConfigType.BOOLEAN,
                description
        );
    }

    private void deleteIfPresent(String key) {
        appConfigRepository.findByConfigKey(key).ifPresent(appConfigRepository::delete);
    }

    private void upsertValue(
            String key,
            String value,
            AppConfig.ConfigType type,
            String description
    ) {
        AppConfig cfg = appConfigRepository.findByConfigKey(key).orElseGet(() -> AppConfig.builder()
                .configKey(key)
                .createdAt(LocalDateTime.now())
                .build());
        cfg.setConfigValue(value);
        cfg.setConfigType(type);
        cfg.setDescription(description);
        cfg.setCategory(CATEGORY);
        cfg.setIsActive(true);
        cfg.setUpdatedAt(LocalDateTime.now());
        appConfigRepository.save(cfg);
    }

    private AppConfig upsertIfMissing(
            String key,
            String defaultValue,
            AppConfig.ConfigType type,
            String description
    ) {
        return appConfigRepository.findByConfigKey(key).orElseGet(() -> appConfigRepository.save(
                AppConfig.builder()
                        .configKey(key)
                        .configValue(defaultValue)
                        .configType(type)
                        .description(description)
                        .category(CATEGORY)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        ));
    }
}
