package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class AddMoneyRequest {
    private double amount;
    private String currency;
}
