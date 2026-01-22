package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config", indexes = {
    @Index(name = "idx_config_key", columnList = "configKey", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String configKey;          // Unique key identifier

    @Column(columnDefinition = "TEXT")
    private String configValue;        // Config value

    @Enumerated(EnumType.STRING)
    private ConfigType configType;     // Type of value (for parsing)

    private String description;        // Config description (for CMS)

    private Boolean isActive;          // Enable/disable config

    // ===== CMS Metadata =====
    private String category;           // GENERAL, PAYMENT, NOTIFICATION, FEATURE, etc.

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
        category = "GENERAL";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ConfigType {
        STRING, INTEGER, BOOLEAN, FLOAT, JSON, LIST, URL, EMAIL, PHONE
    }

    // ===== Common Config Keys (Documentation) =====
    /*
    Common Configuration Keys:
    
    GENERAL:
    - app.version: Current app version
    - app.maintenance_mode: Maintenance mode on/off
    - app.support_email: Support email
    
    PAYMENT:
    - payment.razorpay_key_id: Razorpay key
    - payment.min_wallet_amount: Minimum wallet top-up
    - payment.max_wallet_amount: Maximum wallet limit
    - payment.commission_percentage: Admin commission %
    
    NOTIFICATION:
    - notification.chat_reminder_minutes: Reminder interval
    - notification.booking_notification_hours: Hours before booking
    - notification.push_enabled: Enable push notifications
    
    FEATURE:
    - feature.video_consultation_enabled: Video feature on/off
    - feature.group_chat_enabled: Group chat enabled
    - feature.kundli_service_enabled: Kundli service
    
    PRICING:
    - pricing.default_consultation_rate: Default rate/min
    - pricing.puja_booking_fee: Booking fee for puja
    */
}
