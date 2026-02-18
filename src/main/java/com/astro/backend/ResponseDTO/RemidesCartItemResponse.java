package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RemidesCartItemResponse {
    private Long id;
    private Long userId;
    private Long remidesId;
    private Integer quantity;
    private String title;
    private String subtitle;
    private String imageBase64;
    private Double price;
    private Double tokenAmount;
    private Double discountPercentage;
    private Double finalPrice;
    private String currency;
}
