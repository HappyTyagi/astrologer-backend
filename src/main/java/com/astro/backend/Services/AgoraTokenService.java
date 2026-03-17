package com.astro.backend.Services;

import com.astro.backend.ResponseDTO.AgoraTokenResponse;
import com.astro.backend.ResponseDTO.AgoraRtmTokenResponse;
import io.agora.rtm.RtmTokenBuilder2;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgoraTokenService {

    @Value("${agora.app-id:}")
    private String appId;

    @Value("${agora.app-certificate:}")
    private String appCertificate;

    @Value("${agora.token-expire-seconds:3600}")
    private Integer tokenExpireSeconds;

    public AgoraTokenResponse generateRtcToken(String channelName, int uid, String roleInput) {
        long now = System.currentTimeMillis() / 1000L;
        int expirySeconds = tokenExpireSeconds == null ? 3600 : tokenExpireSeconds;
        if (expirySeconds <= 0) {
            expirySeconds = 3600;
        }
        long expireAt = now + expirySeconds;

        if (isBlank(appId)) {
            return AgoraTokenResponse.builder()
                    .success(false)
                    .message("Agora app-id is missing in backend configuration")
                    .appId("")
                    .token("")
                    .channelName(channelName)
                    .uid(uid)
                    .generatedAtEpoch(now)
                    .expiresAtEpoch(expireAt)
                    .tokenRequired(true)
                    .build();
        }

        if (isBlank(appCertificate)) {
            return AgoraTokenResponse.builder()
                    .success(true)
                    .message("Agora app-certificate missing. Returning empty token (works only when App Certificate is disabled in Agora project).")
                    .appId(appId.trim())
                    .token("")
                    .channelName(channelName)
                    .uid(uid)
                    .generatedAtEpoch(now)
                    .expiresAtEpoch(expireAt)
                    .tokenRequired(false)
                    .build();
        }

        RtcTokenBuilder2.Role role = "subscriber".equalsIgnoreCase(roleInput)
                ? RtcTokenBuilder2.Role.ROLE_SUBSCRIBER
                : RtcTokenBuilder2.Role.ROLE_PUBLISHER;

        String token = new RtcTokenBuilder2().buildTokenWithUid(
                appId.trim(),
                appCertificate.trim(),
                channelName,
                uid,
                role,
                expirySeconds,
                expirySeconds
        );

        return AgoraTokenResponse.builder()
                .success(true)
                .message("Agora RTC token generated")
                .appId(appId.trim())
                .token(token)
                .channelName(channelName)
                .uid(uid)
                .generatedAtEpoch(now)
                .expiresAtEpoch(expireAt)
                .tokenRequired(true)
                .build();
    }

    public AgoraRtmTokenResponse generateRtmToken(String rtmUserId) {
        long now = System.currentTimeMillis() / 1000L;
        int expirySeconds = tokenExpireSeconds == null ? 3600 : tokenExpireSeconds;
        if (expirySeconds <= 0) {
            expirySeconds = 3600;
        }
        long expireAt = now + expirySeconds;

        if (isBlank(appId)) {
            return AgoraRtmTokenResponse.builder()
                    .success(false)
                    .message("Agora app-id is missing in backend configuration")
                    .appId("")
                    .token("")
                    .rtmUserId(rtmUserId)
                    .generatedAtEpoch(now)
                    .expiresAtEpoch(expireAt)
                    .tokenRequired(true)
                    .build();
        }

        if (isBlank(appCertificate)) {
            return AgoraRtmTokenResponse.builder()
                    .success(true)
                    .message("Agora app-certificate missing. Returning empty RTM token (works only when App Certificate is disabled in Agora project).")
                    .appId(appId.trim())
                    .token("")
                    .rtmUserId(rtmUserId)
                    .generatedAtEpoch(now)
                    .expiresAtEpoch(expireAt)
                    .tokenRequired(false)
                    .build();
        }

        String token = new RtmTokenBuilder2().buildToken(
                appId.trim(),
                appCertificate.trim(),
                rtmUserId,
                expirySeconds
        );

        return AgoraRtmTokenResponse.builder()
                .success(true)
                .message("Agora RTM token generated")
                .appId(appId.trim())
                .token(token)
                .rtmUserId(rtmUserId)
                .generatedAtEpoch(now)
                .expiresAtEpoch(expireAt)
                .tokenRequired(true)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
