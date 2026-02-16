package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp_transactions", indexes = {
        @Index(name = "idx_email_otp_email", columnList = "email"),
        @Index(name = "idx_email_otp_ref", columnList = "refNumber", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false, length = 64, unique = true)
    private String refNumber;

    @Column(nullable = false)
    private Boolean isVerified;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime consumedAt;

    @PrePersist
    protected void onCreate() {
        if (isVerified == null) {
            isVerified = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}

