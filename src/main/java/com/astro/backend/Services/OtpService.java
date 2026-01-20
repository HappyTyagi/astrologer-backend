package com.astro.backend.Services;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redis;

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    private final Random random = new Random();

    public String generateOtp(String email) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        redis.opsForValue().set(email, otp, expiryMinutes, TimeUnit.MINUTES);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = redis.opsForValue().get(email);
        if (storedOtp == null) return false;
        return storedOtp.equals(otp);
    }
}
