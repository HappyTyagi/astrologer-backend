package com.astro.backend.RequestDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSupportCallStatusRequest {
    private String status;
    private Integer endedBy;
}
