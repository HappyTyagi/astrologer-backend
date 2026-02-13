package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.AgoraTokenRequest;
import com.astro.backend.ResponseDTO.AgoraTokenResponse;
import com.astro.backend.Services.AgoraTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/call")
@RequiredArgsConstructor
public class CallTokenController {

    private final AgoraTokenService agoraTokenService;

    @PostMapping("/agora-token")
    public ResponseEntity<AgoraTokenResponse> generateAgoraToken(@Valid @RequestBody AgoraTokenRequest request) {
        try {
            AgoraTokenResponse response = agoraTokenService.generateRtcToken(
                    request.getChannelName().trim(),
                    request.getUid(),
                    request.getRole()
            );
            if (!response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AgoraTokenResponse.builder()
                            .success(false)
                            .message("Failed to generate Agora token: " + e.getMessage())
                            .token("")
                            .appId("")
                            .tokenRequired(true)
                            .build());
        }
    }
}
