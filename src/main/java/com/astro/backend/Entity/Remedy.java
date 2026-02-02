package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "remedies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remedy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long birthChartId;  // Foreign key to BirthChart

    @Column(nullable = false)
    private String remedyType;  // "Gemstone", "Rudraksha", "Mantra", "Color", "Day", "Direction"

    @Column(nullable = false)
    private String remedyFor;  // "Mangal Dosha", "Weak Sun", "Saturn affliction", etc.

    private String recommendation;  // e.g., "Yellow Sapphire", "11 mukhi Rudraksha", "Om Namah Shivaya"

    private String details;  // Additional details

    private String benefitDescription;  // What wearing it will help with

    private Integer price;  // Suggested price range

    private String source;  // Where to buy or how to perform

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
