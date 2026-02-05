package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class KundliRequest {
    private double lat;
    private double lon;
    private int dd;
    private int mm;
    private int yyyy;
    private double time; // HH.MM (e.g., 14.5 for 14:30)
}
