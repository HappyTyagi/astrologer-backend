package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteAccountRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
