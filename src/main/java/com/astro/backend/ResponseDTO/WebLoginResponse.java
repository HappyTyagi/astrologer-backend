package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebLoginResponse {
    private Long id;
    private String email;
    private String name;
    private String mobileNumber;
    private String profileImageUrl;
    private String role;
    private String token;
    private Boolean status;
    private String message;
}
