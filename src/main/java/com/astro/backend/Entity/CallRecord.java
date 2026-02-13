package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String chatId;

    @Column(nullable = false)
    private String callId;

    @Column(nullable = false)
    private Integer callerUserId;

    @Column(nullable = false)
    private Integer receiverUserId;

    @Column(nullable = false)
    private String callType; // audio | video

    @Column(nullable = false)
    private String endReason; // local_end | remote_end | reject | missed

    @Column(nullable = false)
    private Integer durationSeconds;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (durationSeconds == null) {
            durationSeconds = 0;
        }
    }
}
