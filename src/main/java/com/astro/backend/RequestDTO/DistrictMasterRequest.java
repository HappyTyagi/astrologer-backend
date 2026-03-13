package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class DistrictMasterRequest {
    
    @NotNull(message = "State ID is required")
    private Long stateId;
    
    @NotBlank(message = "District name is required")
    private String name;

    @JsonAlias("hi_name")
    private String hiName;
    
    private String code;
    
    private String description;
    
    private Boolean isActive;
}
