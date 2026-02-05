package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AstroDashboardResponse {
    private Boolean status;
    private String message;

    private Long userId;
    private String name;
    private String mobileNo;

    private String sunSign;
    private String moonSign;
    private String lagnaSign;
    private String nakshatra;

    private String luckyColor;
    private Integer luckyNumber;
    private String luckyDay;
    private String luckyGemstone;
    private String auspiciousTime;

    private PlanetaryPositionResponse planetaryPositions;
}
