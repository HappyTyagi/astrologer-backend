package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanetaryPositionResponse {
    private Integer statusCode;
    private InputData input;
    private List<Map<String, Object>> output;
    private Long chartId;  // Database record ID
    private String svgUrl;  // URL to access the saved SVG file
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputData {
        private Integer year;
        private Integer month;
        private Integer date;
        private Integer hours;
        private Integer minutes;
        private Integer seconds;
        private Double latitude;
        private Double longitude;
        private Double timezone;
        private ConfigData config;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigData {
        private String observationPoint;
        private String ayanamsha;
    }
}
