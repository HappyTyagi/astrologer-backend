package com.astro.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vedic_astrology_rules",
        indexes = {
                @Index(name = "idx_vedic_rule_category", columnList = "category"),
                @Index(name = "idx_vedic_rule_subcategory", columnList = "subcategory"),
                @Index(name = "idx_vedic_rule_active", columnList = "isActive")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VedicAstrologyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String ruleCode;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 120)
    private String subcategory;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "title_hindi", nullable = false, length = 300)
    private String titleHindi;

    @Lob
    @Column(name = "description_en", nullable = false, columnDefinition = "LONGTEXT")
    private String descriptionEn;

    @Lob
    @Column(name = "description_hi", nullable = false, columnDefinition = "LONGTEXT")
    private String descriptionHi;

    @Column(length = 700)
    private String tags;

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
