package com.astro.backend.RequestDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanetaryPositionRequest {
    private Integer year;
    private Integer month;
    private Integer date;
    private Integer hours;
    private Integer minutes;
    private Integer seconds;
    private Double latitude;
    private Double longitude;
    private Double timezone;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String observationPoint;  // "topocentric" or "geocentric"
        private String ayanamsha;         // "lahiri", etc.
    }
    
    private Config config;
}
