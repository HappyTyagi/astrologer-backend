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
public class AstrologerDistrictPriceResponse {

    private Long id;

    private Long astrologerId;

    private Long districtMasterId;

    private Long pujaId;

    private Double consultationPrice;

    private Double discountPercentage;

    private Double finalPrice;

    private Boolean isActive;

    private String notes;

    private Integer minBookings;

    private Integer maxCapacity;

    private LocalDateTime validFrom;

    private LocalDateTime validTill;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean status;

    private String message;
}
