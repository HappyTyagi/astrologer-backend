package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgoraRtmTokenResponse {
    private boolean success;
    private String message;
    private String appId;
    private String token;
    private String rtmUserId;
    private long generatedAtEpoch;
    private long expiresAtEpoch;
    private boolean tokenRequired;
}
