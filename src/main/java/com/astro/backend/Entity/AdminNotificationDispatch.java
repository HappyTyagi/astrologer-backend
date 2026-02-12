package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification_dispatch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderUserId;

    @Column(nullable = false)
    private String senderEmail;

    @Column(nullable = false)
    private String audienceType; // BROADCAST or INDIVIDUAL

    private Long targetUserId;

    private String targetUserName;

    private String targetMobileNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String notificationType;

    @Column(nullable = false)
    private Integer requestedCount;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false)
    private Integer failedCount;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requestedCount == null) requestedCount = 0;
        if (successCount == null) successCount = 0;
        if (failedCount == null) failedCount = 0;
    }
}
