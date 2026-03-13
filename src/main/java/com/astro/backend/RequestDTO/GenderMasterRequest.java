package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class GenderMasterRequest {
    
    @NotBlank(message = "Gender name is required")
    private String name;

    @JsonAlias("hi_name")
    private String hiName;
    
    private String description;
    
    private Boolean isActive;
}
