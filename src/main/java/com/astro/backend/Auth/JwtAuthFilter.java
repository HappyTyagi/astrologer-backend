package com.astro.backend.Auth;


import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.Entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter implements Filter {

    public static final String AUTH_USER_ID_ATTR = "AUTH_USER_ID";
    private final JwtService jwtService;
    private final UserRepository userRepo;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (!jwtService.isTokenValid(token)) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                String subject = jwtService.extractEmail(token);
                String normalizedSubject = subject == null ? "" : subject.trim();
                if (normalizedSubject.isEmpty()) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                java.util.Optional<User> userOptional;
                if (normalizedSubject.contains("@")) {
                    userOptional = userRepo.findByEmail(normalizedSubject);
                } else {
                    userOptional = userRepo.findByMobileNumber(normalizedSubject);
                }

                userOptional.ifPresent(user -> {
                    var authorities = user.getRole() == null
                            ? List.<SimpleGrantedAuthority>of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                    var auth = new UsernamePasswordAuthenticationToken(
                            user.getEmail() != null ? user.getEmail() : user.getMobileNumber(),
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    req.setAttribute(AUTH_USER_ID_ATTR, user.getId());
                });
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
