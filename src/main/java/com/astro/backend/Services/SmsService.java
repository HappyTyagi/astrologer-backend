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
        try {
            String message = URLEncoder.encode("Your OTP is " + otp, "UTF-8");
            String data = "apikey=" + apiKey
                    + "&numbers=" + mobile
                    + "&message=" + message
                    + "&sender=" + sender;

            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.textlocal.in/send/?").openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(data.getBytes("UTF-8"));

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            System.out.println("SMS Response: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

