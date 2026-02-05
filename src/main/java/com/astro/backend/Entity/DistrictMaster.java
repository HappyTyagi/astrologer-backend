package com.astro.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "district_master", indexes = {
    @Index(name = "idx_district_location", columnList = "latitude, longitude"),
    @Index(name = "idx_district_state", columnList = "stateId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stateId;  // Foreign key to StateMaster

    @Column(nullable = false, unique = true)
    private String name;  // District name

    private String code;  // District code

    private String description;

    private Double latitude;  // District center latitude

    private Double longitude;  // District center longitude

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
