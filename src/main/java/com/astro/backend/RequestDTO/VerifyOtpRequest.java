package com.astro.backend.RequestDTO;


import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
}
