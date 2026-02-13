package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgoraTokenRequest {

    @NotBlank(message = "Channel name is required")
    private String channelName;

    @NotNull(message = "UID is required")
    @Min(value = 1, message = "UID must be greater than 0")
    private Integer uid;

    private String role; // publisher/subscriber
}
