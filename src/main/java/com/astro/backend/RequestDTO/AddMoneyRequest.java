package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class AddMoneyRequest {
    private int amount;
    private String currency;
}
