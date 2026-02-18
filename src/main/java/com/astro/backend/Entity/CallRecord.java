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

    private Integer totalMinutes;

    private Integer freeMinutesApplied;

    private Integer billableMinutes;

    private Double ratePerMinute;

    private Integer billingUnitMinutes;

    private Integer billedUnits;

    private Double ratePerUnit;

    private Double chargedAmount;

    private Integer chargedUserId;

    private Boolean walletDebited;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (durationSeconds == null) {
            durationSeconds = 0;
        }
        if (totalMinutes == null) {
            totalMinutes = 0;
        }
        if (freeMinutesApplied == null) {
            freeMinutesApplied = 0;
        }
        if (billableMinutes == null) {
            billableMinutes = 0;
        }
        if (ratePerMinute == null) {
            ratePerMinute = 0.0;
        }
        if (chargedAmount == null) {
            chargedAmount = 0.0;
        }
        if (billingUnitMinutes == null) {
            billingUnitMinutes = 30;
        }
        if (billedUnits == null) {
            billedUnits = 0;
        }
        if (ratePerUnit == null) {
            ratePerUnit = 0.0;
        }
        if (walletDebited == null) {
            walletDebited = false;
        }
    }
}
