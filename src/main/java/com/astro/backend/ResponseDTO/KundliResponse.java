package com.astro.backend.ResponseDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KundliResponse {
    private List<PlanetPosition> planets;
    private String ascendant;
    private String nakshatra;
}