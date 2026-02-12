package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WebAdminPujaRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "1.0", message = "price must be greater than 0")
    private Double price;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be greater than 0")
    private Integer durationMinutes;

    @NotBlank(message = "category is required")
    private String category;

    @NotBlank(message = "benefits is required")
    private String benefits;

    @NotBlank(message = "rituals is required")
    private String rituals;

    @NotBlank(message = "image is required")
    private String image;

    @NotNull(message = "popupEnabled is required")
    private Boolean popupEnabled;

    @NotNull(message = "popupStartDate is required")
    private LocalDate popupStartDate;

    @NotNull(message = "popupEndDate is required")
    private LocalDate popupEndDate;

    private Integer popupPriority;
    private String popupLabel;
    private String status;
    private Long astrologerId;
    private Boolean isFeatured;
}
