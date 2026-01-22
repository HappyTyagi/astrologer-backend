package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "state_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // State name

    private String code;  // State code (e.g., MH for Maharashtra)

    private String country;  // Country

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
