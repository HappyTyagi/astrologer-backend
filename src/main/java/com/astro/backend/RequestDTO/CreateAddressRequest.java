package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAddressRequest {

    @NotBlank(message = "User mobile number is required")
    private String userMobileNumber;

    private String name;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    private String district;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private String landmark;

    private String addressType;

    private Boolean isDefault;
}
