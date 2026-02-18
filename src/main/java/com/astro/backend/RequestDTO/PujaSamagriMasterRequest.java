package com.astro.backend.RequestDTO;

import lombok.Data;

@Data
public class PujaSamagriMasterRequest {
    private String name;
    private String description;
    private Boolean isActive;
}
