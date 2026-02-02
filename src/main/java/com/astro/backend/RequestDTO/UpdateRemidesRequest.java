package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class UpdateRemidesRequest {
    private Long userId;
    private String title;
    private String description;
    private Double price;
    private Double discountPercentage;
    private String currency;
    private String imageBase64;
}
