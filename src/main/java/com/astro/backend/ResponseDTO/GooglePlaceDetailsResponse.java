package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GooglePlaceDetailsResponse {
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String state;
    private String city;
    private String pincode;
    private String provider;
}
