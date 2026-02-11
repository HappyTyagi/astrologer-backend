package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_transactions", indexes = {
    @Index(name = "idx_mobile", columnList = "mobileNumber"),
    @Index(name = "idx_ref_number", columnList = "refNumber", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String mobileNumber;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false, length = 50, unique = true)
    private String refNumber;          // UUID session reference (increased to 50 for UUID format)

    @Column(nullable = false)
    private Boolean isVerified;        // OTP verification status

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime expiresAt;   // OTP expiry time

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime verifiedAt;  // When OTP was verified
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minutes expiry
        isVerified = false;
    }

    // Check if OTP is still valid (not expired)
    public boolean isValid() {
        return LocalDateTime.now().isBefore(expiresAt) && !isVerified;
    }
}
