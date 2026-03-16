package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nakshatra_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NakshatraMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 120)
    private String slug;

    @Column(name = "hi_name")
    private String hiName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Lob
    @Column(name = "description_hi", columnDefinition = "LONGTEXT")
    private String descriptionHi;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @JsonIgnore
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
