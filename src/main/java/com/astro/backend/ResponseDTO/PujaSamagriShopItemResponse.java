package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PujaSamagriShopItemResponse {
    private Long id;
    private String name;
    private String hiName;
    private String description;
    private String hiDescription;
    private Double price;
    private Double discountPercentage;
    private Double finalPrice;
    private String currency;
    private String imageUrl;
    private List<String> images;
    private Boolean shopEnabled;
}

