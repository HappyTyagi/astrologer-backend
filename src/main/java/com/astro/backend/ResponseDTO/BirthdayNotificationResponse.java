package com.astro.backend.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayNotificationResponse {

    private Long id;

    private Long userId;

    private String userFullName;

    private String userEmail;

    private String userMobileNumber;

    private String userProfileImage;

    private Integer upcomingYear;

    private String title;

    private String message;

    private String templateBody;

    private String templateImageUrl;

    private String templateIconUrl;

    private Boolean emailSent;

    private LocalDateTime emailSentAt;

    private Boolean appNotificationSent;

    private LocalDateTime appNotificationSentAt;

    private Boolean isViewed;

    private LocalDateTime viewedAt;

    private Double discountPercentage;

    private String discountCode;

    private String offerDescription;

    private LocalDateTime offerValidTill;

    private Boolean isSent;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean statusCode;

    private String message_resp;
}
