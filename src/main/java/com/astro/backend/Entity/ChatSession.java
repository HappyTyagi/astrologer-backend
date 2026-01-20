package com.astro.backend.Entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long astrologerId;

    private double ratePerMin; // 20 INR
    private String currency; // INR

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private int totalMinutes;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        STARTED, ENDED, AUTO_ENDED, CANCELED
    }
}

