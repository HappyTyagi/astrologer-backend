package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class UpdateRemidesRequest {
    private Long userId;
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
    private Double price;
    private Double tokenAmount;
    private Double discountPercentage;
    private String currency;
    private String imageBase64;
}
