package com.astro.backend.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmPushService {

    private static final String FIREBASE_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fcm.enabled:true}")
    private boolean enabled;

    @Value("${fcm.project-id:}")
    private String configuredProjectId;

    @Value("${fcm.service-account-file:}")
    private String serviceAccountFile;

    @Value("${fcm.service-account-json:}")
    private String serviceAccountJson;

    @Value("${fcm.service-account-base64:}")
    private String serviceAccountBase64;

    public PushResult sendToToken(
            String fcmToken,
            String title,
            String body,
            String type,
            String imageUrl,
            String actionUrl,
            String actionData
    ) {
        if (!enabled) return PushResult.fail("FCM is disabled");
        if (isBlank(fcmToken)) return PushResult.fail("Missing FCM token");

        try {
            CredentialsAndProject credentialsAndProject = loadCredentialsAndProject();
            if (credentialsAndProject.credentials == null) {
                return PushResult.fail("FCM credentials not configured");
            }
            if (isBlank(credentialsAndProject.projectId)) {
                return PushResult.fail("FCM project ID missing (fcm.project-id or service account project_id)");
            }

            AccessToken accessToken = credentialsAndProject.credentials.getAccessToken();
            if (accessToken == null || accessToken.getExpirationTime() == null ||
                    accessToken.getExpirationTime().toInstant().isBefore(Instant.now().plusSeconds(60))) {
                credentialsAndProject.credentials.refresh();
                accessToken = credentialsAndProject.credentials.getAccessToken();
            }

            if (accessToken == null || isBlank(accessToken.getTokenValue())) {
                return PushResult.fail("Unable to get Google OAuth access token for FCM");
            }

            String endpoint = "https://fcm.googleapis.com/v1/projects/" +
                    credentialsAndProject.projectId.trim() + "/messages:send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken.getTokenValue());

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("token", fcmToken.trim());

            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("title", safe(title));
            notification.put("body", safe(body));
            if (isHttpImage(imageUrl)) notification.put("image", imageUrl.trim());
            message.put("notification", notification);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("title", safe(title));
            data.put("message", safe(body));
            data.put("type", safe(type));
            if (!isBlank(actionUrl)) data.put("actionUrl", actionUrl.trim());
            if (!isBlank(actionData)) data.put("actionData", actionData.trim());
            if (!isBlank(imageUrl)) data.put("imageUrl", imageUrl.trim());
            message.put("data", data);

            Map<String, Object> android = new LinkedHashMap<>();
            android.put("priority", "HIGH");
            message.put("android", android);

            Map<String, Object> apns = new LinkedHashMap<>();
            apns.put("payload", Map.of("aps", Map.of("sound", "default")));
            message.put("apns", apns);

            Map<String, Object> payload = Map.of("message", message);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            String raw = response.getBody() == null ? "" : response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && raw.contains("\"name\"")) {
                return PushResult.ok(raw);
            }
            return PushResult.fail("FCM v1 send failed: HTTP " + response.getStatusCode().value(), raw);

        } catch (HttpStatusCodeException e) {
            return PushResult.fail("FCM v1 HTTP error: " + e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return PushResult.fail("FCM v1 transport error: " + e.getMessage());
        } catch (Exception e) {
            return PushResult.fail("FCM v1 unexpected error: " + e.getMessage());
        }
    }

    private CredentialsAndProject loadCredentialsAndProject() {
        try {
            GoogleCredentials credentials;
            String projectId = blankToNull(configuredProjectId);

            if (!isBlank(serviceAccountFile)) {
                try (InputStream in = openCredentialsStream(serviceAccountFile.trim())) {
                    if (in == null) {
                        return new CredentialsAndProject(null, null);
                    }
                    credentials = GoogleCredentials.fromStream(in).createScoped(List.of(FIREBASE_SCOPE));
                }
            } else if (!isBlank(serviceAccountBase64)) {
                byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64.trim());
                try (InputStream in = new ByteArrayInputStream(decoded)) {
                    credentials = GoogleCredentials.fromStream(in).createScoped(List.of(FIREBASE_SCOPE));
                }
                if (projectId == null) {
                    projectId = extractProjectIdFromJsonBytes(decoded);
                }
            } else if (!isBlank(serviceAccountJson)) {
                byte[] bytes = serviceAccountJson.getBytes(StandardCharsets.UTF_8);
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    credentials = GoogleCredentials.fromStream(in).createScoped(List.of(FIREBASE_SCOPE));
                }
                if (projectId == null) {
                    projectId = extractProjectIdFromJsonBytes(bytes);
                }
            } else {
                credentials = GoogleCredentials.getApplicationDefault().createScoped(List.of(FIREBASE_SCOPE));
            }

            if (projectId == null && credentials instanceof ServiceAccountCredentials sac) {
                projectId = blankToNull(sac.getProjectId());
            }

            return new CredentialsAndProject(credentials, projectId);
        } catch (Exception e) {
            return new CredentialsAndProject(null, null);
        }
    }

    private InputStream openCredentialsStream(String filePath) throws Exception {
        if (filePath.startsWith("classpath:")) {
            String resourcePath = filePath.substring("classpath:".length()).trim();
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        }
        return new FileInputStream(filePath);
    }

    private String extractProjectIdFromJsonBytes(byte[] bytes) {
        try {
            Map<?, ?> map = objectMapper.readValue(bytes, Map.class);
            Object projectId = map.get("project_id");
            return projectId == null ? null : blankToNull(projectId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String value) {
        String safeValue = safe(value).trim();
        return safeValue.isEmpty() ? null : safeValue;
    }

    private boolean isHttpImage(String value) {
        if (isBlank(value)) return false;
        String v = value.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static class PushResult {
        private boolean success;
        private String reason;
        private String rawResponse;

        public static PushResult ok(String rawResponse) {
            return of(true, "SENT", rawResponse);
        }

        public static PushResult fail(String reason) {
            return of(false, reason, null);
        }

        public static PushResult fail(String reason, String rawResponse) {
            return of(false, reason, rawResponse);
        }
    }

    private record CredentialsAndProject(GoogleCredentials credentials, String projectId) {}
}
