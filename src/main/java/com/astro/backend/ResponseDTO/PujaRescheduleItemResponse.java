package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PujaRescheduleItemResponse {
    private Long bookingId;
    private Long pujaId;
    private String pujaName;
    private Long currentSlotId;
    private LocalDateTime currentSlotTime;
    private String bookingStatus;
    private Double totalPrice;
    private Boolean canReschedule;
    private LocalDateTime rescheduleAllowedTill;
    private String rescheduleMessage;
}

