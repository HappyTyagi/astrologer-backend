package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatPricingUpdateRequest {

    @NotNull(message = "chatRatePerMin is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "chatRatePerMin must be >= 0")
    private Double chatRatePerMin;

    @NotNull(message = "freeMinutes is required")
    @Min(value = 0, message = "freeMinutes must be >= 0")
    private Integer freeMinutes;

    @NotNull(message = "chatAllowed is required")
    private Boolean chatAllowed;
}
