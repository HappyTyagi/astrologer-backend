package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CallRecordRequest {

    @NotBlank(message = "chatId is required")
    private String chatId;

    @NotBlank(message = "callId is required")
    private String callId;

    @NotNull(message = "callerUserId is required")
    private Integer callerUserId;

    @NotNull(message = "receiverUserId is required")
    private Integer receiverUserId;

    @NotBlank(message = "callType is required")
    private String callType;

    @NotBlank(message = "endReason is required")
    private String endReason;

    @NotNull(message = "durationSeconds is required")
    @Min(value = 0, message = "durationSeconds must be >= 0")
    private Integer durationSeconds;

    private String startedAtIso;
    private String endedAtIso;
}
