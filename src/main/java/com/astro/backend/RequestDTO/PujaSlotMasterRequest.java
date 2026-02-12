package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaSlotMasterRequest {
    private Long pujaId;
    private String startDate;      // YYYY-MM-DD
    private String endDate;        // YYYY-MM-DD
    private String dayStartTime;   // HH:mm
    private String dayEndTime;     // HH:mm
    private Integer gapMinutes;    // default 30
    private Long astrologerId;
    private Integer maxBookings;   // default 1
    private Boolean isRecurring;   // optional
    private String recurringPattern;
}

