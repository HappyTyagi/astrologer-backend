package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendOtpResponse {
    private String sessionId;          // Session ID for verification
    private String message;            // Status message
    private String mobileNo;           // Mobile number (masked)
    private Boolean success;           // Success status
}
