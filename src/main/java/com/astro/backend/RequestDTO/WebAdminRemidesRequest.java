package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WebAdminRemidesRequest {

    private Long userId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "1.0", message = "price must be greater than 0")
    private Double price;

    @NotNull(message = "tokenAmount is required")
    @DecimalMin(value = "0.1", message = "tokenAmount must be greater than 0")
    private Double tokenAmount;

    @DecimalMin(value = "0.0", message = "discountPercentage cannot be negative")
    private Double discountPercentage;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "imageBase64 is required")
    private String imageBase64;

    private Boolean isActive;
}
