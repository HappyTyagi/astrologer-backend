package com.astro.backend.Services;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

@Service
public class SmsService {

    @Value("${textlocal.api-key}")
    private String apiKey;

    @Value("${textlocal.sender}")
    private String sender;

    public void sendOtpSms(String mobile, String otp) {
        sendTextMessage(mobile, "Your OTP is " + otp);
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
}
