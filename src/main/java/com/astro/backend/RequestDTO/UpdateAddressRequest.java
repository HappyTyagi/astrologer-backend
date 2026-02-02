package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class UpdateAddressRequest {

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
}
