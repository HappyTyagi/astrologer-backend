package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PujaSamagriCartItemResponse {
    private Long id;
    private Long userId;
    private Long samagriMasterId;
    private Integer quantity;
    private String name;
    private String hiName;
    private String description;
    private String hiDescription;
    private String imageUrl;
    private Double price;
    private Double discountPercentage;
    private Double finalPrice;
    private String currency;
}
