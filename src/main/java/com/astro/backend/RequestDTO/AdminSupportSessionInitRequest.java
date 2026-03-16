package com.astro.backend.RequestDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSupportSessionInitRequest {
    private String userName;
    private String userPhone;
    private String userAvatar;
}
