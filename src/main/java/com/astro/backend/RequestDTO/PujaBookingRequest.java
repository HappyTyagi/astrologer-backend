package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaBookingRequest {
    private Long userId;
    private Long pujaId;
    private Long slotId;
    private Long addressId;
}
