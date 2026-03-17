package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRemidesRequest {
    private Long userId;

    @NotBlank(message = "Title is required")
    private String title;

    @JsonAlias({"titleHindi", "title_hindi", "titleHi", "title_hi", "hiTitle", "hi_title", "hiName", "hi_name"})
    private String titleHindi;

    private String subtitle;

    @JsonAlias({"subtitleHindi", "subtitle_hindi", "subtitleHi", "subtitle_hi", "hiSubtitle", "hi_subtitle"})
    private String subtitleHindi;

    private String description;

    @JsonAlias({"descriptionHindi", "description_hindi", "descriptionHi", "description_hi", "hiDescription", "hi_description"})
    private String descriptionHindi;

    private String category;

    @JsonAlias({"categoryHindi", "category_hindi", "categoryHi", "category_hi", "hiCategory", "hi_category"})
    private String categoryHindi;

    @NotNull(message = "Price is required")
    private Double price;

    @DecimalMin(value = "0.1", message = "Token amount must be greater than 0")
    private Double tokenAmount;

    private Double discountPercentage;

    private String currency;

    private String imageBase64;
}
