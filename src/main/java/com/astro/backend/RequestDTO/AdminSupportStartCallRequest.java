package com.astro.backend.RequestDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSupportStartCallRequest {
    private String initiatorName;
    private String callType;
}
