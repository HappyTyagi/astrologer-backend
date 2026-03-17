package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "puja_samagri_master_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaSamagriMasterImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "samagri_master_id", nullable = false)
    @JsonIgnore
    private PujaSamagriMaster samagriMaster;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder;

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
        if (displayOrder == null) {
            displayOrder = 0;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
        }
    }
}
