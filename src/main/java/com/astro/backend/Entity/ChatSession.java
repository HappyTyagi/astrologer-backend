package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


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

    private double ratePerMin; // Rate per minute
    private String currency;   // INR
    private Integer freeMinutesAllowed; // Free minutes before billing starts

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private int totalMinutes;

    @Enumerated(EnumType.STRING)
    private Status status;

    // ===== Enhanced Management =====
    private Double amountCharged;      // Actual amount deducted
    private Double walletUsed;         // Wallet amount used
    private String callType;           // CHAT, VIDEO, VOICE, TEXT
    private Integer chatHistoryCount;  // Number of messages
    private String endReason;          // Why chat ended

    // ===== CMS & Analytics =====
    private Integer userRating;        // Rating by user 1-5
    private Integer astrologerRating;  // Rating by astrologer 1-5
    private String userFeedback;
    private String astrologerFeedback;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (freeMinutesAllowed == null || freeMinutesAllowed < 0) {
            freeMinutesAllowed = 0;
        }
        if (totalMinutes < 0) {
            totalMinutes = 0;
        }
        createdAt = LocalDateTime.now();
    }

    public enum Status {
        STARTED, ENDED, AUTO_ENDED, CANCELED
    }
}
