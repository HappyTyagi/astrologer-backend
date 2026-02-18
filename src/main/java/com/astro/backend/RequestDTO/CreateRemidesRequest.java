package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRemidesRequest {
    private Long userId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Price is required")
    private Double price;

    @DecimalMin(value = "0.1", message = "Token amount must be greater than 0")
    private Double tokenAmount;

    private Double discountPercentage;

    private String currency;

    private String imageBase64;
}
