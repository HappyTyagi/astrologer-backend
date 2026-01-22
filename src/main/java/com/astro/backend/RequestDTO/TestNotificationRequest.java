package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class TestNotificationRequest {
    
    @NotBlank(message = "Label is required")
    private String label;
    
    @NotBlank(message = "Test message is required")
    private String testMessage;
}
