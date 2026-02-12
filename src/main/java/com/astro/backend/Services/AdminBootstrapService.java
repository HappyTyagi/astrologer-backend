package com.astro.backend.Services;

import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .orElse(null);

        if (admin == null) {
            User created = User.builder()
                    .name("System Admin")
                    .email("admin@astrologer.local")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .isVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(created);
            log.info("✅ Admin bootstrap user created with email admin@astrologer.local");
            return;
        }

        boolean updated = false;
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            admin.setEmail("admin@astrologer.local");
            updated = true;
        }
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            updated = true;
        }
        if (Boolean.FALSE.equals(admin.getIsActive())) {
            admin.setIsActive(true);
            updated = true;
        }
        if (updated) {
            userRepository.save(admin);
            log.info("✅ Admin bootstrap user updated with secure login credentials");
        }
    }
}
