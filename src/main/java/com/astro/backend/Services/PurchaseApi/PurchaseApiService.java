package com.astro.backend.Services.PurchaseApi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PurchaseApiService {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public PurchaseApiService(
            RestTemplate restTemplate,
            @Value("${free-astrology.base-url}") String baseUrl,
            @Value("${free-astrology.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public ResponseEntity<String> post(String path, JsonNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-api-key", apiKey);

        String payload = body == null ? null : body.toString();
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        String url = baseUrl + path;
        return restTemplate.postForEntity(url, entity, String.class);
    }
}