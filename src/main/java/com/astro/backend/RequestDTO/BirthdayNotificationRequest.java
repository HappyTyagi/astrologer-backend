package com.astro.backend.RequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BirthdayNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String title;

    private String message;

    private String templateImageUrl;

    private String templateIconUrl;

    private Double discountPercentage;

    private String discountCode;

    private String offerDescription;

    private LocalDateTime offerValidTill;
}
