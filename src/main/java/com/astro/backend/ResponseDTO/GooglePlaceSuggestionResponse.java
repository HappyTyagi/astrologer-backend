package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GooglePlaceSuggestionResponse {
    private String placeId;
    private String description;
    private String provider;
    private Double latitude;
    private Double longitude;
    private String state;
    private String city;
    private String pincode;
}
