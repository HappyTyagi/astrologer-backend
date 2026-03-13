package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class StateMasterRequest {
    
    @NotBlank(message = "State name is required")
    private String name;

    @JsonAlias("hi_name")
    private String hiName;
    
    private String code;
    
    private String country;
    
    private String description;
    
    private Boolean isActive;
}
