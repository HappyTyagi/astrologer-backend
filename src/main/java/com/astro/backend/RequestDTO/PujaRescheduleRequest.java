package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaRescheduleRequest {
    private Long userId;
    private Long bookingId;
    private Long newSlotId;
}

