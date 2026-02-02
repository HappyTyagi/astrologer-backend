package com.astro.backend.ResponseDTO;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullKundliResponse {

    private Long chartId;
    private String name;
    private String dateOfBirth;
    private String timeOfBirth;
    private Double latitude;
    private Double longitude;

    // Basic information
    private String lagna;
    private String sunSign;
    private String moonSign;
    private String nakshatra;
    private String pada;

    // Planets
    @ToString.Exclude
    private List<PlanetPosition> planets;

    // Houses
    @ToString.Exclude
    private Map<Integer, String> houses;  // House number -> Sign

    // Divisional Charts
    @ToString.Exclude
    private String navamsaChart;  // D9 chart JSON
    @ToString.Exclude
    private String dashamsa;  // D10 chart JSON

    // Panchang
    @ToString.Exclude
    private Map<String, Object> panchang;

    // Doshas with details
    private Dosha mangalDosha;
    private Dosha kaalSarpDosha;
    private Dosha pitruDosha;
    private Dosha grahanDosha;

    // Yogas
    private List<String> auspiciousYogas;
    private List<String> inauspiciousYogas;

    // Elements distribution
    private Map<String, Double> elements;

    // Dasha
    @ToString.Exclude
    private DashaResponse vimshottariDasha;

    // Remedies
    @ToString.Exclude
    private Map<String, Object> remedies;

    // Overall health indicators
    private String overallMessage;
    private Integer healthScore;  // 0-100

    // Location metadata
    private LocationMeta locationMeta;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Dosha {
        private Boolean present;
        private String description;
        private String remedyAdvice;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationMeta {
        private String place;
        private String timezone;
        private String ayanamsa;
        private Double julianDay;
    }

}
