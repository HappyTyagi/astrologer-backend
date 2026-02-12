package com.astro.backend.Contlorer.Web;

import com.astro.backend.Auth.JwtService;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.AuthRequest;
import com.astro.backend.ResponseDTO.WebLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/web/auth")
@RequiredArgsConstructor
public class WebLoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<WebLoginResponse> login(@RequestBody AuthRequest authRequest) {
        try {
            String email = authRequest.getEmail().trim();
            String password = authRequest.getPassword();

            // Validate inputs
            if (email.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body(WebLoginResponse.builder()
                        .status(false)
                        .message("Email and password are required")
                        .build());
            }

            // Find user by email
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(WebLoginResponse.builder()
                        .status(false)
                        .message("Invalid email or password")
                        .build());
            }

            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(WebLoginResponse.builder()
                        .status(false)
                        .message("Only admin can login to web panel")
                        .build());
            }

            if (Boolean.FALSE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(WebLoginResponse.builder()
                        .status(false)
                        .message("Admin account is inactive")
                        .build());
            }

            // Login is blocked when password is not set in User entity
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(WebLoginResponse.builder()
                        .status(false)
                        .message("Password is not set for this admin account")
                        .build());
            }

            // Verify password (and auto-migrate plain text to BCrypt if needed)
            String storedPassword = user.getPassword() == null ? "" : user.getPassword();
            boolean passwordMatched;
            try {
                passwordMatched = passwordEncoder.matches(password, storedPassword);
            } catch (Exception ignored) {
                passwordMatched = false;
            }
            if (!passwordMatched && storedPassword.equals(password)) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                passwordMatched = true;
            }

            if (!passwordMatched) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(WebLoginResponse.builder()
                        .status(false)
                        .message("Invalid email or password")
                        .build());
            }

            // Generate JWT token (15 minutes expiry)
            long tokenExpiry = 1000 * 60 * 15;
            String token = jwtService.generateToken(user.getEmail(), tokenExpiry);

            // Return response with user info and token
            return ResponseEntity.ok(WebLoginResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .mobileNumber(user.getMobileNumber())
                    .role(user.getRole().toString())
                    .token(token)
                    .status(true)
                    .message("Login successful")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(WebLoginResponse.builder()
                    .status(false)
                    .message("Error during login: " + e.getMessage())
                    .build());
        }
    }
}
