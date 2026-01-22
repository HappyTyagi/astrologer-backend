package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateMasterResponse {
    private Long id;
    private String name;
    private String code;
    private String country;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean status;
    private String message;
}
