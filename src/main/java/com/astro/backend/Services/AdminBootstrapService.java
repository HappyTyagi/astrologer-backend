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

    private static final String FIXED_ADMIN_NAME = "Astro Adhya";
    private static final String FIXED_ADMIN_EMAIL = "admin@astrologer.local";
    private static final String FIXED_ADMIN_MOBILE = "8057700080";
    private static final String FIXED_ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User fixedAdmin = userRepository.findByMobileNumber(FIXED_ADMIN_MOBILE)
                .orElseGet(() -> User.builder()
                        .name(FIXED_ADMIN_NAME)
                        .email(FIXED_ADMIN_EMAIL)
                        .mobileNumber(FIXED_ADMIN_MOBILE)
                        .password(passwordEncoder.encode(FIXED_ADMIN_PASSWORD))
                        .role(Role.ADMIN)
                        .isVerified(true)
                        .isActive(true)
                        .build());

        boolean fixedAdminUpdated = false;
        if (fixedAdmin.getRole() != Role.ADMIN) {
            fixedAdmin.setRole(Role.ADMIN);
            fixedAdminUpdated = true;
        }
        if (fixedAdmin.getEmail() == null || fixedAdmin.getEmail().isBlank()) {
            fixedAdmin.setEmail(FIXED_ADMIN_EMAIL);
            fixedAdminUpdated = true;
        }
        if (fixedAdmin.getPassword() == null || fixedAdmin.getPassword().isBlank()) {
            fixedAdmin.setPassword(passwordEncoder.encode(FIXED_ADMIN_PASSWORD));
            fixedAdminUpdated = true;
        }
        if (Boolean.FALSE.equals(fixedAdmin.getIsActive())) {
            fixedAdmin.setIsActive(true);
            fixedAdminUpdated = true;
        }
        if (Boolean.FALSE.equals(fixedAdmin.getIsVerified())) {
            fixedAdmin.setIsVerified(true);
            fixedAdminUpdated = true;
        }

        if (fixedAdmin.getId() == null || fixedAdminUpdated) {
            fixedAdmin = userRepository.save(fixedAdmin);
            log.info("Fixed admin ensured for mobile {}", FIXED_ADMIN_MOBILE);
        }

        // Keep this number as the only ADMIN account; demote other admins to USER.
        for (User user : userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN)) {
            if (!user.getId().equals(fixedAdmin.getId())) {
                user.setRole(Role.USER);
                userRepository.save(user);
                log.info("Demoted previous admin {} ({}) to USER", user.getName(), user.getMobileNumber());
            }
        }
    }
}
