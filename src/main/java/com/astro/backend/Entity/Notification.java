package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;               // Recipient user

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;     // BOOKING, PAYMENT, SESSION, PROMO, REMINDER

    private Boolean isRead;            // Read status
    private LocalDateTime readAt;      // When read

    private String actionUrl;          // Deep link for mobile app
    private String actionData;         // JSON data for action

    private String imageUrl;           // For rich notifications

    // ===== Delivery Tracking =====
    private String deliveryStatus;     // PENDING, SENT, FAILED, DELIVERED
    private LocalDateTime sentAt;
    private String failureReason;      // Why delivery failed

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime expiresAt;   // Auto-delete after expiry

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRead = false;
        deliveryStatus = "PENDING";
        expiresAt = LocalDateTime.now().plusDays(30); // 30 days expiry
    }

    public enum NotificationType {
        BOOKING, PAYMENT, SESSION, PROMO, REMINDER, RATING_REQUEST, CHAT_MESSAGE, ADMIN_ALERT
    }
}
