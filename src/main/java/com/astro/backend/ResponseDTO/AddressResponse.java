package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AddressResponse {

    private Long id;
    private String userMobileNumber;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String district;
    private String pincode;
    private String landmark;
    private String addressType;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean status;
    private String message;
}
