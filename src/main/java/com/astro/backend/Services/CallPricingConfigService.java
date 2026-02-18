package com.astro.backend.Services;

import com.astro.backend.Entity.AppConfig;
import com.astro.backend.Repositry.AppConfigRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CallPricingConfigService {

    public static final int BILLING_UNIT_MINUTES = 30;
    public static final String CATEGORY = "CALL_PRICING";
    public static final String KEY_AUDIO_RATE = "call.audio.rate_per_min";
    public static final String KEY_VIDEO_RATE = "call.video.rate_per_min";
    public static final String KEY_AUDIO_FREE_MIN = "call.audio.free_minutes";
    public static final String KEY_VIDEO_FREE_MIN = "call.video.free_minutes";

    private static final double DEFAULT_AUDIO_RATE = 3.0;
    private static final double DEFAULT_VIDEO_RATE = 5.0;
    private static final int DEFAULT_AUDIO_FREE_MIN = 0;
    private static final int DEFAULT_VIDEO_FREE_MIN = 0;

    private final AppConfigRepository appConfigRepository;

    @Getter
    @Builder
    public static class CallPricingConfig {
        private double audioRatePerMin;
        private double videoRatePerMin;
        private int audioFreeMinutes;
        private int videoFreeMinutes;
    }

    @Transactional
    public CallPricingConfig getOrCreateConfig() {
        return CallPricingConfig.builder()
                .audioRatePerMin(readDouble(KEY_AUDIO_RATE, DEFAULT_AUDIO_RATE, "Audio call charge in INR per 30 minutes"))
                .videoRatePerMin(readDouble(KEY_VIDEO_RATE, DEFAULT_VIDEO_RATE, "Video call charge in INR per 30 minutes"))
                .audioFreeMinutes(readInt(KEY_AUDIO_FREE_MIN, DEFAULT_AUDIO_FREE_MIN, "Audio call free minutes allowed"))
                .videoFreeMinutes(readInt(KEY_VIDEO_FREE_MIN, DEFAULT_VIDEO_FREE_MIN, "Video call free minutes allowed"))
                .build();
    }

    @Transactional
    public CallPricingConfig saveConfig(double audioRatePerMin, double videoRatePerMin, int audioFreeMinutes, int videoFreeMinutes) {
        if (audioRatePerMin < 0 || videoRatePerMin < 0) {
            throw new RuntimeException("Rate cannot be negative");
        }
        if (audioFreeMinutes < 0 || videoFreeMinutes < 0) {
            throw new RuntimeException("Free minutes cannot be negative");
        }
        upsertDouble(KEY_AUDIO_RATE, audioRatePerMin, "Audio call charge in INR per 30 minutes");
        upsertDouble(KEY_VIDEO_RATE, videoRatePerMin, "Video call charge in INR per 30 minutes");
        upsertInt(KEY_AUDIO_FREE_MIN, audioFreeMinutes, "Audio call free minutes allowed");
        upsertInt(KEY_VIDEO_FREE_MIN, videoFreeMinutes, "Video call free minutes allowed");
        return getOrCreateConfig();
    }

    public boolean isVideo(String callType) {
        return "video".equalsIgnoreCase(normalizeCallType(callType));
    }

    public String normalizeCallType(String callType) {
        String normalized = callType == null ? "" : callType.trim().toLowerCase(Locale.ROOT);
        return "video".equals(normalized) ? "video" : "audio";
    }

    private double readDouble(String key, double defaultValue, String description) {
        AppConfig cfg = upsertIfMissing(key, String.valueOf(defaultValue), AppConfig.ConfigType.FLOAT, description);
        try {
            return Double.parseDouble(String.valueOf(cfg.getConfigValue()).trim());
        } catch (Exception ignored) {
            cfg.setConfigValue(String.valueOf(defaultValue));
            cfg.setUpdatedAt(LocalDateTime.now());
            appConfigRepository.save(cfg);
            return defaultValue;
        }
    }

    private int readInt(String key, int defaultValue, String description) {
        AppConfig cfg = upsertIfMissing(key, String.valueOf(defaultValue), AppConfig.ConfigType.INTEGER, description);
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(cfg.getConfigValue()).trim()));
        } catch (Exception ignored) {
            cfg.setConfigValue(String.valueOf(defaultValue));
            cfg.setUpdatedAt(LocalDateTime.now());
            appConfigRepository.save(cfg);
            return defaultValue;
        }
    }

    private void upsertDouble(String key, double value, String description) {
        upsertValue(key, String.valueOf(value), AppConfig.ConfigType.FLOAT, description);
    }

    private void upsertInt(String key, int value, String description) {
        upsertValue(key, String.valueOf(Math.max(0, value)), AppConfig.ConfigType.INTEGER, description);
    }

    private void upsertValue(String key, String value, AppConfig.ConfigType type, String description) {
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

    private AppConfig upsertIfMissing(String key, String defaultValue, AppConfig.ConfigType type, String description) {
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
