package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;
    private Long senderId;
    private String message;
    private LocalDateTime timestamp;

    // ===== Message Management =====
    private Boolean isRead;            // Message read status
    private LocalDateTime readAt;      // When message was read
    private String messageType;        // TEXT, IMAGE, AUDIO, DOCUMENT
    private String mediaUrl;           // URL if media
    private Boolean isEdited;          // Message edited status
    private LocalDateTime editedAt;
    private Long replyToMessageId;     // For message threading
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
