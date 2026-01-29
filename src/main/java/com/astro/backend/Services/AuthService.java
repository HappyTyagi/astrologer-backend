package com.astro.backend.Services;


import com.astro.backend.Auth.JwtService;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.AuthRequest;
import com.astro.backend.RequestDTO.RegisterRequest;
import com.astro.backend.ResponseDTO.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .role(Role.USER)
                .build();

        User savedUser = userRepo.save(user);

        return generateTokens(savedUser);
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return generateTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        return generateTokens(user);
    }

    private AuthResponse generateTokens(User user) {
        long accessExpiry = 1000 * 60 * 15;  // 15 min
        long refreshExpiry = 1000L * 60 * 60 * 24 * 30; // 30 days

        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .accessToken(jwtService.generateToken(user.getEmail(), accessExpiry))
                .refreshToken(jwtService.generateToken(user.getEmail(), refreshExpiry))
                .role(user.getRole().name())
                .status(true)
                .message("Authentication successful")
                .build();
    }
}
