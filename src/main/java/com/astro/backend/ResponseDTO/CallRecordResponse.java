package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallRecordResponse {
    private boolean success;
    private String message;
    private Long id;
}
