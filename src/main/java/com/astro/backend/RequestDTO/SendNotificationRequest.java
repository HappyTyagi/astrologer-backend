package com.astro.backend.RequestDTO;

import com.astro.backend.Entity.Notification;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SendNotificationRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private Notification.NotificationType type;  // BOOKING, PAYMENT, SESSION, PROMO, REMINDER, etc.
    
    private String imageUrl;           // Banner/image URL for rich notification
    
    private String actionUrl;          // Deep link for app
    
    private String actionData;         // JSON data for action
}
