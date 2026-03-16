package com.astro.backend.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class PujaSamagriCartSyncRequest {
    private Long userId;
    private List<CartItem> items;

    @Data
    public static class CartItem {
        private Long samagriMasterId;
        private Integer quantity;
    }
}
