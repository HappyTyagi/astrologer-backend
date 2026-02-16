package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long senderUserId;
    private String audienceType; // INDIVIDUAL or BROADCAST

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Boolean isRead;
    private LocalDateTime readAt;

    private String actionUrl;
    private String actionData;
    private String imageUrl;

    private String deliveryStatus;
    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
        if (deliveryStatus == null || deliveryStatus.isBlank()) deliveryStatus = "PENDING";
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusDays(30);
    }

    public enum NotificationType {
        BOOKING, PAYMENT, SESSION, PROMO, REMINDER, RATING_REQUEST, CHAT_MESSAGE, ADMIN_ALERT
    }
}

