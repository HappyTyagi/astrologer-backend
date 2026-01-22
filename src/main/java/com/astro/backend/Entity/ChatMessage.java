package com.astro.backend.Entity;


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

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
