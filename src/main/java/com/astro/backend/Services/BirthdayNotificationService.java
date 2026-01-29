package com.astro.backend.Services;

import com.astro.backend.Entity.BirthdayNotification;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.User;
import com.astro.backend.Entity.Notification;
import com.astro.backend.Repositry.BirthdayNotificationRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.Repositry.NotificationRepository;
import com.astro.backend.RequestDTO.BirthdayNotificationRequest;
import com.astro.backend.ResponseDTO.BirthdayNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BirthdayNotificationService {

    private final BirthdayNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final EmailService emailService;
    private final NotificationRepository notificationRepo;

    /**
     * Scheduler: Check for upcoming birthdays daily
     * Runs at 8:00 AM every day
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkUpcomingBirthdays() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrowDate = today.plusDays(1);  // Check for tomorrow's birthdays
        Integer currentYear = today.getYear();

        // Get all mobile user profiles from database
        List<MobileUserProfile> allProfiles = mobileUserProfileRepository.findAll();

        for (MobileUserProfile profile : allProfiles) {
            if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isEmpty()) {
                continue;
            }

            try {
                // Parse user's date of birth
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate dob = LocalDate.parse(profile.getDateOfBirth(), formatter);
                
                // Get birthday in current/next year
                LocalDate upcomingBirthday = dob.withYear(currentYear);
                if (upcomingBirthday.isBefore(today)) {
                    upcomingBirthday = dob.withYear(currentYear + 1);
                }

                // Check if birthday is tomorrow
                if (upcomingBirthday.equals(tomorrowDate)) {
                    // Get user details
                    User user = userRepository.findById(profile.getUserId()).orElse(null);
                    if (user == null) continue;
                    
                    // Check if notification already exists for this year
                    var existingNotification = notificationRepository.findByUserAndYear(user.getId(), upcomingBirthday.getYear());
                    
                    if (existingNotification.isEmpty()) {
                        createBirthdayNotification(user, profile, upcomingBirthday);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing birthday for profile " + profile.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Create birthday notification for a user
     */
    private void createBirthdayNotification(User user, MobileUserProfile profile, LocalDate birthdayDate) {
        String title = "Happy Birthday, " + user.getName() + "! 🎉";
        String message = "Celebrate your special day with exclusive birthday discounts and special offers!";
        
        BirthdayNotification notification = BirthdayNotification.builder()
                .userId(user.getId())
                .userFullName(user.getName())
                .userEmail(user.getEmail())
                .userMobileNumber(user.getMobileNumber())
                .userProfileImage(profile.getProfileImageUrl())
                .upcomingYear(birthdayDate.getYear())
                .title(title)
                .message(message)
                .templateBody(buildNotificationTemplate(user.getName(), message))
                .templateImageUrl("https://via.placeholder.com/600x300?text=Happy+Birthday")  // Default birthday image
                .templateIconUrl("🎂")
                .discountPercentage(15.0)  // Default 15% birthday discount
                .discountCode("BIRTHDAY" + user.getId())
                .offerDescription("Get 15% off on all consultations on your birthday!")
                .offerValidTill(birthdayDate.plusDays(7).atTime(23, 59, 59))  // Valid for 7 days
                .status("PENDING")
                .build();

        BirthdayNotification savedNotification = notificationRepository.save(notification);
        
        // Send notifications (email and app)
        sendBirthdayNotifications(savedNotification);
    }

    /**
     * Send birthday notifications via email and app
     */
    private void sendBirthdayNotifications(BirthdayNotification notification) {
        try {
            // Send Email
            if (notification.getUserEmail() != null && !notification.getUserEmail().isEmpty()) {
                try {
                    String emailContent = String.format(
                            "<h1>%s</h1><p>%s</p><p>Use code: <strong>%s</strong> for %s%% off!</p>",
                            notification.getTitle(),
                            notification.getMessage(),
                            notification.getDiscountCode(),
                            notification.getDiscountPercentage().intValue()
                    );
                    
                    emailService.sendEmail(
                            notification.getUserEmail(),
                            notification.getTitle(),
                            emailContent
                    );
                    
                    notification.setEmailSent(true);
                    notification.setEmailSentAt(LocalDateTime.now());
                } catch (Exception e) {
                    System.err.println("Failed to send birthday email: " + e.getMessage());
                }
            }

            // Send App Notification
            try {
                Notification appNotif = Notification.builder()
                        .userId(notification.getUserId())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .type(Notification.NotificationType.PROMO)
                        .imageUrl(notification.getTemplateImageUrl())
                        .actionData("{\"notificationId\": " + notification.getId() + "}")
                        .createdAt(LocalDateTime.now())
                        .build();
                notificationRepo.save(appNotif);
                
                notification.setAppNotificationSent(true);
                notification.setAppNotificationSentAt(LocalDateTime.now());
            } catch (Exception e) {
                System.err.println("Failed to send app notification: " + e.getMessage());
            }

            // Update status
            if (notification.getEmailSent() || notification.getAppNotificationSent()) {
                notification.setStatus("SENT");
                notification.setIsSent(true);
            }

            notificationRepository.save(notification);
        } catch (Exception e) {
            notification.setFailureReason(e.getMessage());
            notification.setStatus("FAILED");
            notificationRepository.save(notification);
        }
    }

    /**
     * Get pending birthday notifications for user (when app opens)
     */
    public List<BirthdayNotificationResponse> getUserBirthdayNotifications(Long userId) {
        List<BirthdayNotification> notifications = notificationRepository.findByUserIdAndIsViewedFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark birthday notification as viewed
     */
    public BirthdayNotificationResponse markAsViewed(Long notificationId) {
        BirthdayNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));

        notification.setIsViewed(true);
        notification.setViewedAt(LocalDateTime.now());
        BirthdayNotification updatedNotification = notificationRepository.save(notification);

        return mapToResponse(updatedNotification);
    }

    /**
     * Get all birthday notifications for user
     */
    public List<BirthdayNotificationResponse> getAllUserNotifications(Long userId) {
        List<BirthdayNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending birthday notifications (Admin)
     */
    public List<BirthdayNotificationResponse> getPendingNotifications() {
        List<BirthdayNotification> notifications = notificationRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Manually create birthday notification (Admin)
     */
    public BirthdayNotificationResponse createManualBirthdayNotification(BirthdayNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for user ID: " + request.getUserId()));

        if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isEmpty()) {
            throw new RuntimeException("User does not have a date of birth set");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dob = LocalDate.parse(profile.getDateOfBirth(), formatter);

        BirthdayNotification notification = BirthdayNotification.builder()
                .userId(user.getId())
                .userFullName(user.getName())
                .userEmail(user.getEmail())
                .userMobileNumber(user.getMobileNumber())
                .userProfileImage(profile.getProfileImageUrl())
                .upcomingYear(LocalDate.now().getYear())
                .title(request.getTitle() != null ? request.getTitle() : "Happy Birthday, " + user.getName() + "! 🎉")
                .message(request.getMessage() != null ? request.getMessage() : "Celebrate your special day!")
                .templateBody(buildNotificationTemplate(user.getName(), request.getMessage()))
                .templateImageUrl(request.getTemplateImageUrl())
                .templateIconUrl(request.getTemplateIconUrl())
                .discountPercentage(request.getDiscountPercentage())
                .discountCode(request.getDiscountCode())
                .offerDescription(request.getOfferDescription())
                .offerValidTill(request.getOfferValidTill())
                .status("PENDING")
                .build();

        BirthdayNotification savedNotification = notificationRepository.save(notification);
        sendBirthdayNotifications(savedNotification);

        return mapToResponse(savedNotification);
    }

    /**
     * Build notification template
     */
    private String buildNotificationTemplate(String userName, String message) {
        return String.format(
                "<div style='text-align: center; font-family: Arial;'>" +
                "<h2>🎉 Happy Birthday, %s! 🎉</h2>" +
                "<p>%s</p>" +
                "<p style='color: #ff6b6b; font-size: 16px;'><strong>Special Birthday Discount Inside!</strong></p>" +
                "</div>",
                userName,
                message
        );
    }

    /**
     * Map entity to response
     */
    private BirthdayNotificationResponse mapToResponse(BirthdayNotification notification) {
        return BirthdayNotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .userFullName(notification.getUserFullName())
                .userEmail(notification.getUserEmail())
                .userMobileNumber(notification.getUserMobileNumber())
                .userProfileImage(notification.getUserProfileImage())
                .upcomingYear(notification.getUpcomingYear())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .templateBody(notification.getTemplateBody())
                .templateImageUrl(notification.getTemplateImageUrl())
                .templateIconUrl(notification.getTemplateIconUrl())
                .emailSent(notification.getEmailSent())
                .emailSentAt(notification.getEmailSentAt())
                .appNotificationSent(notification.getAppNotificationSent())
                .appNotificationSentAt(notification.getAppNotificationSentAt())
                .isViewed(notification.getIsViewed())
                .viewedAt(notification.getViewedAt())
                .discountPercentage(notification.getDiscountPercentage())
                .discountCode(notification.getDiscountCode())
                .offerDescription(notification.getOfferDescription())
                .offerValidTill(notification.getOfferValidTill())
                .isSent(notification.getIsSent())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .statusCode(true)
                .message_resp("Success")
                .build();
    }
}
