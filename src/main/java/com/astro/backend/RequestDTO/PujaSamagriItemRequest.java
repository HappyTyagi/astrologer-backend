package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaSamagriItemRequest {
    private Long samagriMasterId;
    private String quantity;
    private String unit;
    private String notes;
    private Integer displayOrder;
    private Boolean isActive;
}
