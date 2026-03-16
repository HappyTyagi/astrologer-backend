package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "admin_support_call_sessions",
        indexes = {
                @Index(name = "idx_admin_support_calls_chat_id", columnList = "chatId"),
                @Index(name = "idx_admin_support_calls_receiver_status", columnList = "receiverId,status"),
                @Index(name = "idx_admin_support_calls_created_at", columnList = "createdAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSupportCallSession {

    @Id
    @Column(nullable = false, length = 120)
    private String id;

    @Column(nullable = false, length = 120)
    private String chatId;

    @Column(nullable = false)
    private Long initiatorId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false)
    private String initiatorName;

    @Column(nullable = false, length = 16)
    private String callType;

    @Column(nullable = false, length = 24)
    private String status;

    private Integer endedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (status == null || status.isBlank()) {
            status = "incoming";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
