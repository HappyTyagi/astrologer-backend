package com.astro.backend.ResponseDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanchangResponse {
    private String tithi;
    private String vara;
    private String nakshatra;
    private String yoga;
    private String karana;
    private String sunrise;
    private String sunset;
}
