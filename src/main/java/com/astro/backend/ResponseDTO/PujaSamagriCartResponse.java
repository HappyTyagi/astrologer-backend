package com.astro.backend.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PujaSamagriCartResponse {
    private Long userId;
    private Integer totalItems;
    private Double totalAmount;
    private Double payableAmount;
    private List<PujaSamagriCartItemResponse> items;
    private LocalDateTime serverTime;
}

