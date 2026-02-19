package com.astro.backend.Services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${textlocal.api-key}")
    private String apiKey;

    @Value("${textlocal.sender}")
    private String sender;

    @Value("${twofactor.enabled:true}")
    private boolean twoFactorEnabled;

    @Value("${twofactor.base-url:https://2factor.in}")
    private String twoFactorBaseUrl;

    @Value("${twofactor.api-key:}")
    private String twoFactorApiKey;

    @Value("${twofactor.otp-template-name:}")
    private String twoFactorOtpTemplateName;

    public void sendOtpSms(String mobile, String otp) {
        if (isTwoFactorConfigured()) {
            boolean sent = sendOtpViaTwoFactor(mobile, otp);
            if (!sent) {
                throw new RuntimeException("2Factor OTP send failed");
            }
            return;
        }
        sendTextMessage(mobile, "Your OTP is " + otp);
    }

    public boolean verifyOtpSms(String mobile, String otp) {
        if (!isTwoFactorConfigured()) {
            return true; // fallback for local/dev
        }
        return verifyOtpViaTwoFactor(mobile, otp);
    }

    public void sendTextMessage(String mobile, String message) {
        try {
            // DUMMY IMPLEMENTATION FOR TESTING
            // In production, replace with actual SMS service

            // For development: Just log the outgoing SMS
            System.out.println("=".repeat(60));
            System.out.println("[DUMMY SMS] Message Sent Successfully");
            System.out.println("Mobile: " + mobile);
            System.out.println("Sender: " + sender);
            System.out.println("Message: " + message);
            System.out.println("=".repeat(60));

            /* PRODUCTION: Uncomment this block when you have TextLocal account
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String data = "apikey=" + apiKey
                    + "&numbers=" + mobile
                    + "&message=" + encodedMessage
                    + "&sender=" + sender;

            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.textlocal.in/send/?").openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);   // 5 seconds connection timeout
            conn.setReadTimeout(5000);      // 5 seconds read timeout
            conn.getOutputStream().write(data.getBytes("UTF-8"));

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            System.out.println("SMS Response: " + result);
            */

        } catch (Exception e) {
            System.err.println("Error in SMS service: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - caller decides retry/fallback
        }
    }

    private boolean sendOtpViaTwoFactor(String mobile, String otp) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(twoFactorBaseUrl)
                    .pathSegment("API", "V1", twoFactorApiKey, "SMS", mobile, otp, twoFactorOtpTemplateName)
                    .build()
                    .encode()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<?, ?> body = response.getBody();
            String status = body == null ? null : String.valueOf(body.get("Status"));
            String details = body == null ? null : String.valueOf(body.get("Details"));

            boolean success = "Success".equalsIgnoreCase(status);
            if (success) {
                log.info("2Factor OTP sent successfully. mobile={}, details={}", mobile, details);
                return true;
            }

            log.warn("2Factor OTP send failed. mobile={}, status={}, details={}", mobile, status, details);
            return false;
        } catch (Exception e) {
            log.error("2Factor OTP send exception. mobile={}, error={}", mobile, e.getMessage());
            return false;
        }
    }

    private boolean verifyOtpViaTwoFactor(String mobile, String otp) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(twoFactorBaseUrl)
                    .pathSegment("API", "V1", twoFactorApiKey, "SMS", "VERIFY3", mobile, otp)
                    .build()
                    .encode()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<?, ?> body = response.getBody();
            String status = body == null ? null : String.valueOf(body.get("Status"));
            String details = body == null ? null : String.valueOf(body.get("Details"));

            boolean success = "Success".equalsIgnoreCase(status)
                    && details != null
                    && details.toLowerCase().contains("otp matched");
            if (!success) {
                log.warn("2Factor OTP verify failed. mobile={}, status={}, details={}", mobile, status, details);
            }
            return success;
        } catch (Exception e) {
            log.error("2Factor OTP verify exception. mobile={}, error={}", mobile, e.getMessage());
            return false;
        }
    }

    private boolean isTwoFactorConfigured() {
        String key = twoFactorApiKey == null ? "" : twoFactorApiKey.trim();
        String template = twoFactorOtpTemplateName == null ? "" : twoFactorOtpTemplateName.trim();
        boolean keyReady = !key.isEmpty() && !key.toUpperCase().startsWith("YOUR_");
        boolean templateReady = !template.isEmpty() && !template.toUpperCase().startsWith("YOUR_");
        return twoFactorEnabled
                && keyReady
                && templateReady;
    }
}
