package com.astro.backend.RequestDTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private Long genderMasterId;       // Gender master ID
    private Long stateMasterId;        // State master ID
    private Long districtMasterId;     // District master ID
}