package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgoraTokenResponse {
    private boolean success;
    private String message;
    private String appId;
    private String token;
    private String channelName;
    private Integer uid;
    private Long expiresAtEpoch;
    private Long generatedAtEpoch;
    private boolean tokenRequired;
}
