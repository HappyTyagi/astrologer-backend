package com.astro.backend.RequestDTO;

import com.astro.backend.Entity.Notification;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendNotificationByMobileRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Notification.NotificationType type;
    private String imageUrl;
    private String actionUrl;
    private String actionData;
}
