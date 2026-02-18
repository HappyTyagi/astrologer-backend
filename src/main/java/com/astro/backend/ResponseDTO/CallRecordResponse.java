package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallRecordResponse {
    private boolean success;
    private String message;
    private Long id;
    private String callType;
    private Integer totalMinutes;
    private Integer freeMinutesApplied;
    private Integer billableMinutes;
    private Integer billingUnitMinutes;
    private Integer billedUnits;
    private Double ratePerUnit;
    private Double ratePerMinute;
    private Double chargedAmount;
    private Integer chargedUserId;
    private Boolean walletDebited;
    private Boolean chargeSkipped;
}
