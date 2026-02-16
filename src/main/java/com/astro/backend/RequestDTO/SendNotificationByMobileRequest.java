package com.astro.backend.RequestDTO;

import com.astro.backend.Entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendNotificationByMobileRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
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
