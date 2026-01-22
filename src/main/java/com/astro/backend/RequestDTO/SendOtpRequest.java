package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 10, message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
}
