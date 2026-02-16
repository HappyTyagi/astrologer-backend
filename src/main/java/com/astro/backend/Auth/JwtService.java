package com.astro.backend.Auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:mySuperSecretKey12345678901234567890}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate JWT token with email as subject
     */
    public String generateToken(String email, long expiryMs) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    /**
     * Generate JWT token with mobile number and name as claims
     */
    public String generateTokenWithClaims(String mobileNumber, String name, long expiryMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("mobileNumber", mobileNumber);
        claims.put("name", name);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(mobileNumber)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    /**
     * Generate non-expiring JWT token with subject only.
     */
    public String generateTokenWithoutExpiry(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .signWith(key)
                .compact();
    }

    /**
     * Generate non-expiring JWT token with mobile claims.
     */
    public String generateTokenWithClaimsWithoutExpiry(String mobileNumber, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("mobileNumber", mobileNumber);
        claims.put("name", name);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(mobileNumber)
                .setIssuedAt(new Date())
                .signWith(key)
                .compact();
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extract mobile number from token
     */
    public String extractMobileNumber(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("mobileNumber");
    }

    /**
     * Extract name from token
     */
    public String extractName(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("name");
    }

    /**
     * Validate token
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
