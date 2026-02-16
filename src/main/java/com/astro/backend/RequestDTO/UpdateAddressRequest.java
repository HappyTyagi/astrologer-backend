package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAddressRequest {

    @Pattern(regexp = "^[0-9]{10}$", message = "User mobile number must be 10 digits")
    private String userMobileNumber;

    private String name;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String district;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    private String landmark;

    private String addressType;

    private Boolean isDefault;
}
