package com.astro.backend.ResponseDTO;


import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanetPosition {
    private String planet;
    private double longitude;
    private double degree;
    private String rashi;
    private double speed;
    private boolean retrograde;
}
