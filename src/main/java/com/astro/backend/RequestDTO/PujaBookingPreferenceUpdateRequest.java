package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaBookingPreferenceUpdateRequest {
    private Long gotraMasterId;
    private String customGotraName;
    private Long rashiMasterId;
    private Long nakshatraMasterId;
}
