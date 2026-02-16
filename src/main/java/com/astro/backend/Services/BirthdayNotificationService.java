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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayNotificationService {

    private final BirthdayNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationRepository notificationRepo;

    /**
     * Scheduler: Check for today's birthdays and anniversaries daily
     * Runs at 12:00 AM IST every day
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")
    public void checkDailyOccasionWishesAtMidnight() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        List<MobileUserProfile> allProfiles = mobileUserProfileRepository.findAll();

        for (MobileUserProfile profile : allProfiles) {
            try {
                User user = userRepository.findById(profile.getUserId()).orElse(null);
                if (user == null) continue;
                if (Boolean.FALSE.equals(user.getPromotionalNotificationsEnabled())) continue;

                processBirthdayWish(profile, user, today, currentYear);
                processAnniversaryWish(profile, user, today, currentYear);
            } catch (Exception e) {
                log.error("Error processing daily wishes for profileId={}", profile.getId(), e);
            }
        }
    }

    private void processBirthdayWish(
            MobileUserProfile profile,
            User user,
            LocalDate today,
            int currentYear
    ) {
        LocalDate dob = parseDateFlexible(profile.getDateOfBirth());
        if (dob == null) return;

        LocalDate birthdayThisYear = dob.withYear(currentYear);
        if (!birthdayThisYear.equals(today)) return;

        String uniqueCode = "BDAY-" + currentYear + "-" + user.getId();
        if (notificationRepository.findFirstByDiscountCode(uniqueCode).isPresent()) return;

        createOccasionNotification(user, profile, birthdayThisYear, OccasionType.BIRTHDAY, uniqueCode);
    }

    private void processAnniversaryWish(
            MobileUserProfile profile,
            User user,
            LocalDate today,
            int currentYear
    ) {
        LocalDate anniversary = parseDateFlexible(profile.getAnniversaryDate());
        if (anniversary == null) return;

        LocalDate anniversaryThisYear = anniversary.withYear(currentYear);
        if (!anniversaryThisYear.equals(today)) return;

        String uniqueCode = "ANNIV-" + currentYear + "-" + user.getId();
        if (notificationRepository.findFirstByDiscountCode(uniqueCode).isPresent()) return;

        createOccasionNotification(user, profile, anniversaryThisYear, OccasionType.ANNIVERSARY, uniqueCode);
    }

    private LocalDate parseDateFlexible(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) return null;
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateValue.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return null;
    }

    private void createOccasionNotification(
            User user,
            MobileUserProfile profile,
            LocalDate eventDate,
            OccasionType occasionType,
            String uniqueCode
    ) {
        String title = occasionType == OccasionType.BIRTHDAY
                ? "Happy Birthday, " + user.getName() + "! 🎉"
                : "Happy Anniversary, " + user.getName() + "! 💖";
        String message = occasionType == OccasionType.BIRTHDAY
                ? "May your day be full of joy, blessings and positivity."
                : "Wishing you love, happiness and a beautiful year ahead.";
        String offerDescription = occasionType == OccasionType.BIRTHDAY
                ? "Get 15% off on all consultations on your birthday!"
                : "Get 10% off on all consultations for your anniversary celebration!";
        double discount = occasionType == OccasionType.BIRTHDAY ? 15.0 : 10.0;

        BirthdayNotification notification = BirthdayNotification.builder()
                .userId(user.getId())
                .userFullName(user.getName())
                .userEmail(user.getEmail())
                .userMobileNumber(user.getMobileNumber())
                .userProfileImage(profile.getProfileImageUrl())
                .upcomingYear(eventDate.getYear())
                .title(title)
                .message(message)
                .templateBody(buildNotificationTemplate(user.getName(), message, occasionType))
                .templateImageUrl(occasionType == OccasionType.BIRTHDAY
                        ? "https://via.placeholder.com/600x300?text=Happy+Birthday"
                        : "https://via.placeholder.com/600x300?text=Happy+Anniversary")
                .templateIconUrl(occasionType == OccasionType.BIRTHDAY ? "🎂" : "💖")
                .discountPercentage(discount)
                .discountCode(uniqueCode)
                .offerDescription(offerDescription)
                .offerValidTill(eventDate.plusDays(7).atTime(23, 59, 59))
                .status("PENDING")
                .build();

        BirthdayNotification savedNotification = notificationRepository.save(notification);
        sendOccasionNotifications(savedNotification, occasionType);
    }

    private void sendOccasionNotifications(BirthdayNotification notification, OccasionType occasionType) {
        try {
            User user = userRepository.findById(notification.getUserId()).orElse(null);
            if (user != null && Boolean.FALSE.equals(user.getPromotionalNotificationsEnabled())) {
                notification.setStatus("SKIPPED");
                notification.setFailureReason("User opted out from promotional notifications");
                notificationRepository.save(notification);
                return;
            }

            boolean smsSent = false;
            boolean emailSent = false;
            boolean appSent = false;

            // Send Email (if email available)
            if (notification.getUserEmail() != null && !notification.getUserEmail().isEmpty()) {
                try {
                    String emailContent = buildOccasionEmailTemplate(
                            notification.getUserFullName(),
                            notification.getMessage(),
                            notification.getDiscountCode(),
                            notification.getDiscountPercentage(),
                            occasionType
                    );

                    emailService.sendEmailAsync(
                            notification.getUserEmail(),
                            notification.getTitle(),
                            emailContent
                    );

                    notification.setEmailSent(true);
                    notification.setEmailSentAt(LocalDateTime.now());
                    emailSent = true;
                } catch (Exception e) {
                    log.error("Failed to queue occasion email. notificationId={}", notification.getId(), e);
                }
            }

            // Send SMS (always preferred when mobile is available)
            if (notification.getUserMobileNumber() != null && !notification.getUserMobileNumber().isBlank()) {
                try {
                    String occasionLabel = occasionType == OccasionType.BIRTHDAY ? "Birthday" : "Anniversary";
                    String smsMessage = String.format(
                            "Happy %s %s! %s Code: %s. Valid till %s.",
                            occasionLabel,
                            notification.getUserFullName(),
                            notification.getMessage(),
                            notification.getDiscountCode(),
                            notification.getOfferValidTill() != null ? notification.getOfferValidTill().toLocalDate() : ""
                    );
                    smsService.sendTextMessage(notification.getUserMobileNumber(), smsMessage);
                    smsSent = true;
                } catch (Exception e) {
                    log.error("Failed to send occasion SMS. notificationId={}", notification.getId(), e);
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
                appSent = true;
            } catch (Exception e) {
                log.error("Failed to save app notification. notificationId={}", notification.getId(), e);
            }

            // Update status
            if (emailSent || appSent || smsSent) {
                notification.setStatus("SENT");
                notification.setIsSent(true);
            } else {
                notification.setStatus("FAILED");
                notification.setFailureReason("No delivery channel succeeded");
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
        if (Boolean.FALSE.equals(user.getPromotionalNotificationsEnabled())) {
            throw new RuntimeException("User has opted out from promotional notifications");
        }

        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Mobile user profile not found for user ID: " + request.getUserId()));

        if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isEmpty()) {
            throw new RuntimeException("User does not have a date of birth set");
        }

        BirthdayNotification notification = BirthdayNotification.builder()
                .userId(user.getId())
                .userFullName(user.getName())
                .userEmail(user.getEmail())
                .userMobileNumber(user.getMobileNumber())
                .userProfileImage(profile.getProfileImageUrl())
                .upcomingYear(LocalDate.now().getYear())
                .title(request.getTitle() != null ? request.getTitle() : "Happy Birthday, " + user.getName() + "! 🎉")
                .message(request.getMessage() != null ? request.getMessage() : "Celebrate your special day!")
                .templateBody(buildNotificationTemplate(
                        user.getName(),
                        request.getMessage() != null ? request.getMessage() : "Celebrate your special day!",
                        OccasionType.BIRTHDAY
                ))
                .templateImageUrl(request.getTemplateImageUrl())
                .templateIconUrl(request.getTemplateIconUrl())
                .discountPercentage(request.getDiscountPercentage())
                .discountCode(request.getDiscountCode())
                .offerDescription(request.getOfferDescription())
                .offerValidTill(request.getOfferValidTill())
                .status("PENDING")
                .build();

        BirthdayNotification savedNotification = notificationRepository.save(notification);
        sendOccasionNotifications(savedNotification, OccasionType.BIRTHDAY);

        return mapToResponse(savedNotification);
    }

    /**
     * Build notification template
     */
    private String buildNotificationTemplate(String userName, String message, OccasionType occasionType) {
        String occasionHeading = occasionType == OccasionType.BIRTHDAY
                ? "Happy Birthday"
                : "Happy Anniversary";
        String icon = occasionType == OccasionType.BIRTHDAY ? "🎂" : "💖";
        return String.format(
                "<div style='text-align:center;font-family:Arial,sans-serif;padding:18px;background:#fff7ef;border:1px solid #ffe2cc;border-radius:14px;'>" +
                "<h2 style='margin:0 0 10px 0;color:#b24c00;'>%s, %s! %s</h2>" +
                "<p style='margin:0;color:#443321;font-size:15px;'>%s</p>" +
                "</div>",
                occasionHeading,
                userName,
                icon,
                message
        );
    }

    private String buildOccasionEmailTemplate(
            String userName,
            String message,
            String discountCode,
            Double discountPercentage,
            OccasionType occasionType
    ) {
        String safeName = userName == null || userName.isBlank() ? "User" : userName;
        String safeMessage = message == null || message.isBlank()
                ? "Celebrate your special day with us."
                : message;
        String code = discountCode == null || discountCode.isBlank() ? "BIRTHDAY" : discountCode;
        String discountText = discountPercentage == null ? "" : discountPercentage.intValue() + "%";
        String heading = occasionType == OccasionType.BIRTHDAY
                ? "Happy Birthday"
                : "Happy Anniversary";
        String icon = occasionType == OccasionType.BIRTHDAY ? "🎂" : "💖";
        String accent = occasionType == OccasionType.BIRTHDAY ? "#ff7a00" : "#c2185b";
        String soft = occasionType == OccasionType.BIRTHDAY ? "#fff4e8" : "#fdeef5";

        return String.format(
                "<div style='font-family:Arial,sans-serif;max-width:640px;margin:0 auto;background:#f7f9fc;padding:20px;'>" +
                "<div style='background:#ffffff;border:1px solid #e4eaf2;border-radius:14px;overflow:hidden;'>" +
                "<div style='padding:18px 20px;background:%s;color:#fff;'>" +
                "<h2 style='margin:0;font-size:23px;'>%s, %s! %s</h2>" +
                "<p style='margin:8px 0 0 0;font-size:14px;opacity:0.95%%;'>Astrologer wishes you positivity and blessings.</p>" +
                "</div>" +
                "<div style='padding:18px 20px;'>" +
                "<p style='margin:0 0 10px 0;font-size:15px;color:#2d3748;'>%s</p>" +
                "<div style='background:%s;border:1px solid #f4d2be;border-radius:10px;padding:12px;'>" +
                "<p style='margin:0;color:#4a2e1f;'><strong>Offer Code:</strong> %s</p>" +
                "<p style='margin:8px 0 0 0;color:#4a2e1f;'><strong>Discount:</strong> %s</p>" +
                "</div>" +
                "<p style='margin-top:14px;color:#667085;font-size:13px;'>This is a system generated greeting from Astrologer.</p>" +
                "</div>" +
                "</div>",
                accent,
                heading,
                safeName,
                icon,
                safeMessage,
                soft,
                code,
                discountText
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

    private enum OccasionType {
        BIRTHDAY,
        ANNIVERSARY
    }
}
