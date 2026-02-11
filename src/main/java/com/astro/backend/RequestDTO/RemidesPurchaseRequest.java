package com.astro.backend.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class RemidesPurchaseRequest {
    private Long userId;
    private List<PurchaseItem> items;

    @Data
    public static class PurchaseItem {
        private Long remidesId;
        private Integer quantity;
    }
}
