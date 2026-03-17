package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RemidesResponse {
    private Long id;
    private Long userId;
    private String title;
    private String titleHindi;
    private String subtitle;
    private String subtitleHindi;
    private String description;
    private String descriptionHindi;
    private String category;
    private String categoryHindi;
    private Double price;
    private Double tokenAmount;
    private Double discountPercentage;
    private Double finalPrice;
    private String currency;
    private String imageBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean status;
    private String message;
}
