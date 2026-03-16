package com.astro.backend.RequestDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSupportMessageActivityRequest {
    private String senderRole;
    private String messageType;
    private String preview;
    private String userName;
    private String userPhone;
    private String userAvatar;
}
