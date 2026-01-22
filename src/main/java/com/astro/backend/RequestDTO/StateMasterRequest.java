package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class StateMasterRequest {
    
    @NotBlank(message = "State name is required")
    private String name;
    
    private String code;
    
    private String country;
    
    private String description;
    
    private Boolean isActive;
}
