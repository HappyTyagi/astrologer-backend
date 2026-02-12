package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String module;
    private String endpoint;
    private String errorType;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Column(columnDefinition = "LONGTEXT")
    private String requestPayload;

    private Long userId;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
