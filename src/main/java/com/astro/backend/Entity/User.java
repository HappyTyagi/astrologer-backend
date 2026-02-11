package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


import com.astro.backend.EnumFile.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Basic Details =====
    @Column(nullable = false)
    private String name;

    private String email;

    @Column(unique = true, nullable = false, length = 10)
    private String mobileNumber;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // ASTROLOGER, ADMIN, USER

    private Boolean isVerified;

    private String country;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
