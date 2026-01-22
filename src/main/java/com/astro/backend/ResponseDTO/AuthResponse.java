package com.astro.backend.ResponseDTO;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private String accessToken;
    private String refreshToken;
    private String role;
    private Long genderMasterId;
    private Long stateMasterId;
    private Long districtMasterId;
    private Boolean status;
    private String message;
}