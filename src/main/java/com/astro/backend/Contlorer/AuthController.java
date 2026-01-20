package com.astro.backend.Contlorer;


import com.astro.backend.RequestDTO.AuthRequest;
import com.astro.backend.RequestDTO.RefreshTokenRequest;
import com.astro.backend.RequestDTO.RegisterRequest;
import com.astro.backend.ResponseDTO.AuthResponse;
import com.astro.backend.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        return service.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshTokenRequest req) {
        return service.refresh(req.getRefreshToken());
    }
}
