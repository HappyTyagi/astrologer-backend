package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_support_chat_sessions",
        indexes = {
                @Index(name = "idx_admin_support_chat_sessions_user_id", columnList = "userId"),
                @Index(name = "idx_admin_support_chat_sessions_last_message_at", columnList = "lastMessageAt"),
                @Index(name = "idx_admin_support_chat_sessions_admin_unread", columnList = "adminUnreadCount")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSupportChatSession {

    @Id
    @Column(nullable = false, length = 120)
    private String chatId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long adminUserId;

    @Column(nullable = false)
    private String rtmChannelName;

    @Column(nullable = false)
    private String userRtmId;

    @Column(nullable = false)
    private String adminRtmId;

    @Column(nullable = false)
    private String userName;

    private String userPhone;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String userAvatar;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String lastMessage;

    @Column(nullable = false)
    private String lastMessageType;

    private LocalDateTime lastMessageAt;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer adminUnreadCount;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer userUnreadCount;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isUserOnline;

    private LocalDateTime userLastSeenAt;

    private LocalDateTime lastReadByAdminAt;

    private LocalDateTime lastReadByUserAt;

    private LocalDateTime userChatClearedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (adminUnreadCount == null) {
            adminUnreadCount = 0;
        }
        if (userUnreadCount == null) {
            userUnreadCount = 0;
        }
        if (isUserOnline == null) {
            isUserOnline = false;
        }
        if (lastMessageType == null || lastMessageType.isBlank()) {
            lastMessageType = "text";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (lastMessageAt == null) {
            lastMessageAt = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
