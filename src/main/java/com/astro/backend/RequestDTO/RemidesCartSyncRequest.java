package com.astro.backend.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class RemidesCartSyncRequest {
    private Long userId;
    private List<CartItem> items;

    @Data
    public static class CartItem {
        private Long remidesId;
        private Integer quantity;
    }
}
