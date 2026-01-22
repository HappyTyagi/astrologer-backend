package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AstrologerDistrictPriceRequest {

    @NotNull(message = "Astrologer ID is required")
    private Long astrologerId;

    @NotNull(message = "District Master ID is required")
    private Long districtMasterId;

    @NotNull(message = "Puja ID is required")
    private Long pujaId;

    @NotNull(message = "Consultation price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double consultationPrice;

    @DecimalMin(value = "0.0", message = "Discount percentage must be >= 0")
    @DecimalMax(value = "100.0", message = "Discount percentage must be <= 100")
    private Double discountPercentage;

    private String notes;

    @Min(value = 0, message = "Min bookings must be >= 0")
    private Integer minBookings;

    @Min(value = 1, message = "Max capacity must be > 0")
    private Integer maxCapacity;

    private String validFrom;  // Format: yyyy-MM-dd HH:mm:ss

    private String validTill;  // Format: yyyy-MM-dd HH:mm:ss (optional)
}
