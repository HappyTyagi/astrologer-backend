package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendOtpResponse {
    private String refNumber;          // Reference number for verification
    private String message;            // Status message
    private Boolean isNewUser;         // Whether new user was created
    private String mobileNumber;       // Mobile number (masked)
}
