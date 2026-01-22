package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class GenderMasterRequest {
    
    @NotBlank(message = "Gender name is required")
    private String name;
    
    private String description;
    
    private Boolean isActive;
}
