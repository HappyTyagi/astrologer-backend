package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gender_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenderMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // MALE, FEMALE, OTHER, etc.

    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
