package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CallPricingUpdateRequest {

    @NotNull(message = "audioRatePerMin is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "audioRatePerMin must be >= 0")
    private Double audioRatePerMin;

    @NotNull(message = "videoRatePerMin is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "videoRatePerMin must be >= 0")
    private Double videoRatePerMin;

    @NotNull(message = "audioFreeMinutes is required")
    @Min(value = 0, message = "audioFreeMinutes must be >= 0")
    private Integer audioFreeMinutes;

    @NotNull(message = "videoFreeMinutes is required")
    @Min(value = 0, message = "videoFreeMinutes must be >= 0")
    private Integer videoFreeMinutes;
}
