package com.astro.backend.Services;


import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.AppConfig;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.GotraMaster;
import com.astro.backend.Entity.NakshatraMaster;
import com.astro.backend.Entity.PujaBookingSpiritualDetail;
import com.astro.backend.Entity.PujaSamagriItem;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Entity.PujaSamagriMasterImage;
import com.astro.backend.Entity.RashiMaster;
import com.astro.backend.Helper.PujaOrderIdHelper;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.AppConfigRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.GotraMasterRepository;
import com.astro.backend.Repositry.NakshatraMasterRepository;
import com.astro.backend.Repositry.PujaBookingSpiritualDetailRepository;
import com.astro.backend.Repositry.PujaSamagriItemRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.Repositry.PujaSamagriMasterImageRepository;
import com.astro.backend.Repositry.RashiMasterRepository;
import com.astro.backend.RequestDTO.PujaSlotMasterRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.RequestDTO.PujaSamagriMasterRequest;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.PujaSlotRepository;
import com.astro.backend.ResponseDTO.PujaRescheduleItemResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Anchor;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PujaService {

    private final PujaRepository pujaRepo;
    private final PujaSlotRepository slotRepo;
    private final PujaBookingRepository bookingRepo;
    private final AddressRepository addressRepository;
    private final WalletService walletService;
    private final OrderHistoryService orderHistoryService;
    private final EmailService emailService;
    private final AppConfigRepository appConfigRepository;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final GotraMasterRepository gotraMasterRepository;
    private final RashiMasterRepository rashiMasterRepository;
    private final NakshatraMasterRepository nakshatraMasterRepository;
    private final PujaBookingSpiritualDetailRepository pujaBookingSpiritualDetailRepository;
    private final PujaSamagriMasterRepository pujaSamagriMasterRepository;
    private final PujaSamagriMasterImageRepository pujaSamagriMasterImageRepository;
    private final PujaSamagriItemRepository pujaSamagriItemRepository;
    private final PujaBookingNotificationService pujaBookingNotificationService;
    private static final LocalTime DEFAULT_DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_DAY_END = LocalTime.of(20, 0);
    private static final int DEFAULT_GAP_MINUTES = 30;
    private static final String BOOKING_FLOW_CATEGORY = "PUJA_BOOKING_FLOW";
    private static final String KEY_MOBILE_SLOT_SELECTION_ENABLED = "puja.mobile.slot_selection.enabled";
    private static final boolean DEFAULT_MOBILE_SLOT_SELECTION_ENABLED = false;
    private static final int JOIN_ALLOWED_MINUTES_BEFORE_SLOT = 10;

    @Value("${app.public-base-url:http://localhost:1234}")
    private String appPublicBaseUrl;

    public String ensurePujaOtp(PujaBooking booking) {
        if (booking == null) {
            return "";
        }
        final String existing = booking.getPujaOtp() == null ? "" : booking.getPujaOtp().trim();
        if (!existing.isEmpty()) {
            return existing;
        }
        final String generated = generatePujaOtp();
        booking.setPujaOtp(generated);
        bookingRepo.save(booking);
        return generated;
    }

    private String generatePujaOtp() {
        // 4-digit OTP for puja start/end confirmation
        return String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
    }

    @Transactional
    public boolean isMobileSlotSelectionEnabled() {
        AppConfig config = appConfigRepository.findByConfigKey(KEY_MOBILE_SLOT_SELECTION_ENABLED)
                .orElseGet(() -> appConfigRepository.save(
                        AppConfig.builder()
                                .configKey(KEY_MOBILE_SLOT_SELECTION_ENABLED)
                                .configValue(String.valueOf(DEFAULT_MOBILE_SLOT_SELECTION_ENABLED))
                                .configType(AppConfig.ConfigType.BOOLEAN)
                                .description("If true, user must select puja slot from mobile booking flow")
                                .category(BOOKING_FLOW_CATEGORY)
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
        String raw = config.getConfigValue() == null ? "" : config.getConfigValue().trim();
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    @Transactional
    public boolean updateMobileSlotSelectionEnabled(Boolean enabled) {
        boolean resolved = Boolean.TRUE.equals(enabled);
        AppConfig config = appConfigRepository.findByConfigKey(KEY_MOBILE_SLOT_SELECTION_ENABLED)
                .orElseGet(() -> AppConfig.builder()
                        .configKey(KEY_MOBILE_SLOT_SELECTION_ENABLED)
                        .createdAt(LocalDateTime.now())
                        .build());
        config.setConfigValue(String.valueOf(resolved));
        config.setConfigType(AppConfig.ConfigType.BOOLEAN);
        config.setDescription("If true, user must select puja slot from mobile booking flow");
        config.setCategory(BOOKING_FLOW_CATEGORY);
        config.setIsActive(true);
        config.setUpdatedAt(LocalDateTime.now());
        appConfigRepository.save(config);
        return resolved;
    }

    private boolean isPujaSlotSelectionEnabled(Puja puja) {
        if (puja == null) {
            return false;
        }
        if (puja.getIsSlot() != null) {
            return Boolean.TRUE.equals(puja.getIsSlot());
        }
        // Backward compatibility for old puja rows where is_slot might be null.
        return isMobileSlotSelectionEnabled();
    }

    private String resolveBookingPackageCode(PujaBooking booking) {
        if (booking == null || booking.getPackageCode() == null) {
            return "BASE";
        }
        String normalized = booking.getPackageCode().trim().toUpperCase(Locale.ROOT);
        if ("REGULAR".equals(normalized) || "PREMIUM".equals(normalized)) {
            return normalized;
        }
        return "BASE";
    }

    private String resolveBookingPackageName(PujaBooking booking) {
        if (booking == null || booking.getPackageName() == null) {
            return resolveBookingPackageCode(booking);
        }
        String name = booking.getPackageName().trim();
        return name.isEmpty() ? resolveBookingPackageCode(booking) : name;
    }

    private boolean isSamagriRequiredForBooking(PujaBooking booking) {
        return "BASE".equals(resolveBookingPackageCode(booking));
    }

    private String normalizeGotraToken(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-/.]+", "");
    }

    private boolean isOtherLikeGotraLabel(String value) {
        final String normalized = normalizeGotraToken(value);
        if (normalized.isEmpty()) {
            return false;
        }
        return "other".equals(normalized)
                || "others".equals(normalized)
                || "unknown".equals(normalized)
                || "na".equals(normalized)
                || "अन्य".equals(normalized)
                || "अन्यगोत्र".equals(normalized)
                || "अन्यगौत्र".equals(normalized);
    }

    private String resolveGotraDisplayName(GotraMaster gotraMaster, String customGotraName) {
        final String masterName = gotraMaster == null || gotraMaster.getName() == null
                ? ""
                : gotraMaster.getName().trim();
        if (!isOtherLikeGotraLabel(masterName)) {
            return masterName;
        }
        final String custom = customGotraName == null ? "" : customGotraName.trim();
        if (custom.isEmpty()) {
            throw new RuntimeException("Please enter gotra name.");
        }
        return custom;
    }

    public PujaBooking bookPuja(
            Long userId,
            Long pujaId,
            Long slotId,
            Long addressId,
            Long gotraMasterId,
            String customGotraName,
            String paymentMethod,
            String transactionId,
            Boolean useWallet,
            String packageCode,
            String packageName,
            Double packagePrice,
            Integer packageDurationMinutes,
            Long rashiMasterId,
            Long nakshatraMasterId
    ) {

        if (addressId == null || addressId <= 0) {
            throw new RuntimeException("Valid addressId is required");
        }
        if (gotraMasterId == null || gotraMasterId <= 0) {
            throw new RuntimeException("Please select gotra.");
        }

        Puja puja = pujaRepo.findById(pujaId)
                .orElseThrow(() -> new RuntimeException("Invalid Puja"));

        final boolean mobileSlotSelectionEnabled = isPujaSlotSelectionEnabled(puja);
        PujaSlot slot = null;
        if (mobileSlotSelectionEnabled) {
            if (slotId == null || slotId <= 0) {
                throw new RuntimeException("Please select puja slot from mobile app.");
            }
            slot = slotRepo.findById(slotId)
                    .orElseThrow(() -> new RuntimeException("Invalid slot selected."));
        }
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));
        GotraMaster gotraMaster = gotraMasterRepository.findById(gotraMasterId)
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .orElseThrow(() -> new RuntimeException("Invalid gotra selected."));
        final String resolvedGotraName = resolveGotraDisplayName(gotraMaster, customGotraName);
        final PujaBookingSpiritualDetail latestSpiritual = pujaBookingSpiritualDetailRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        final Long fallbackRashiId = latestSpiritual == null ? null : latestSpiritual.getRashiMasterId();
        final Long fallbackNakshatraId = latestSpiritual == null ? null : latestSpiritual.getNakshatraMasterId();

        final Long resolvedRashiId = rashiMasterId != null && rashiMasterId > 0 ? rashiMasterId : fallbackRashiId;
        final Long resolvedNakshatraId = nakshatraMasterId != null && nakshatraMasterId > 0
                ? nakshatraMasterId
                : fallbackNakshatraId;

        RashiMaster rashiMaster = null;
        if (resolvedRashiId != null && resolvedRashiId > 0) {
            rashiMaster = rashiMasterRepository.findById(resolvedRashiId)
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .orElse(null);
            if (rashiMaster == null && rashiMasterId != null && rashiMasterId > 0) {
                throw new RuntimeException("Invalid rashi selected");
            }
        }

        NakshatraMaster nakshatraMaster = null;
        if (resolvedNakshatraId != null && resolvedNakshatraId > 0) {
            nakshatraMaster = nakshatraMasterRepository.findById(resolvedNakshatraId)
                    .filter(n -> Boolean.TRUE.equals(n.getIsActive()))
                    .orElse(null);
            if (nakshatraMaster == null && nakshatraMasterId != null && nakshatraMasterId > 0) {
                throw new RuntimeException("Invalid nakshatra selected");
            }
        }

        if (slot != null) {
            if (!Objects.equals(slot.getPujaId(), pujaId)) {
                throw new RuntimeException("Selected slot does not belong to this puja.");
            }
            if (Boolean.FALSE.equals(slot.getIsActive())) {
                throw new RuntimeException("Slot is not active. Please choose another slot.");
            }
            if (slot.getSlotTime() == null || !slot.getSlotTime().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Selected slot is expired. Please choose another slot.");
            }
            if (slot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
                throw new RuntimeException("Slot not available");
            }
        }

        final String normalizedMethod = paymentMethod == null ? "WALLET" : paymentMethod.trim().toUpperCase();
        final boolean wantsWallet = Boolean.TRUE.equals(useWallet);
        final boolean isWalletPayment = "WALLET".equals(normalizedMethod);
        final boolean isGatewayPayment = "GATEWAY".equals(normalizedMethod)
                || "UPI".equals(normalizedMethod)
                || "CARD".equals(normalizedMethod)
                || "NETBANKING".equals(normalizedMethod);

        if (!isWalletPayment && !isGatewayPayment) {
            throw new RuntimeException("Invalid paymentMethod. Use WALLET or GATEWAY.");
        }

        String finalTransactionId = transactionId == null ? "" : transactionId.trim();
        String finalPaymentMethod = normalizedMethod;
        final String resolvedPackageCode = packageCode == null
                ? "BASE"
                : packageCode.trim().toUpperCase(Locale.ROOT);
        final String normalizedPackageCode =
                ("REGULAR".equals(resolvedPackageCode) || "PREMIUM".equals(resolvedPackageCode))
                        ? resolvedPackageCode
                        : "BASE";
        final String resolvedPackageName = packageName == null || packageName.trim().isEmpty()
                ? normalizedPackageCode
                : packageName.trim();
        final double baseAmount = puja.getPrice();
        final double resolvedAmount = packagePrice == null ? baseAmount : packagePrice;
        final double pujaAmount = resolvedAmount > 0
                ? Math.max(resolvedAmount, baseAmount)
                : baseAmount;
        final Integer resolvedPackageDuration = packageDurationMinutes == null || packageDurationMinutes <= 0
                ? puja.getDurationMinutes()
                : packageDurationMinutes;
        if (isWalletPayment) {
            boolean debited = walletService.debit(
                    userId,
                    pujaAmount,
                    "PUJA_BOOKING",
                    "Puja booking (" + resolvedPackageCode + "): " + puja.getName()
            );

            if (!debited) {
                throw new RuntimeException("Insufficient wallet balance. Please add money to wallet or continue with gateway payment.");
            }
            if (finalTransactionId.isEmpty()) {
                finalTransactionId = "WALLET-" + UUID.randomUUID();
            }
            finalPaymentMethod = "WALLET";
        } else {
            double walletUsed = 0.0;
            if (wantsWallet) {
                Wallet wallet = walletService.getWallet(userId);
                double balance = wallet.getBalance();
                if (balance > 0) {
                    walletUsed = walletService.debitUpTo(
                            userId,
                            pujaAmount,
                            "PUJA_BOOKING",
                            "Puja booking (wallet part, " + resolvedPackageCode + "): " + puja.getName()
                    );
                }
            }

            final double remaining = Math.max(0.0, pujaAmount - walletUsed);
            if (remaining > 0 && finalTransactionId.isEmpty()) {
                throw new RuntimeException("Gateway transactionId is required for remaining amount.");
            }
            if (remaining <= 0) {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "WALLET-" + UUID.randomUUID();
                }
                finalPaymentMethod = "WALLET";
            } else {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "GW-" + UUID.randomUUID();
                }
                finalPaymentMethod = walletUsed > 0 ? "WALLET+GATEWAY" : normalizedMethod;
            }
        }

        // Update slot only when mobile slot selection mode is enabled.
        if (slot != null) {
            slot.setStatus(PujaSlot.SlotStatus.BOOKED);
            slotRepo.save(slot);
        }

        // Create booking
        PujaBooking booking = PujaBooking.builder()
                .userId(userId)
                .pujaId(pujaId)
                .slotId(slot == null ? null : slot.getId())
                .address(address)
                .bookedAt(LocalDateTime.now())
                .status(PujaBooking.BookingStatus.CONFIRMED)
                .totalPrice(pujaAmount)
                .packageCode(normalizedPackageCode)
                .packageName(resolvedPackageName)
                .packagePrice(pujaAmount)
                .packageDurationMinutes(resolvedPackageDuration)
                .paymentMethod(finalPaymentMethod)
                .transactionId(finalTransactionId)
                .meetingLink(defaultGoogleMeetLink())
                .meetLinkGeneratedAt(LocalDateTime.now())
                .pujaOtp(generatePujaOtp())
                .notificationStatus("PENDING")
                .slotSelectedByMobile(mobileSlotSelectionEnabled && slot != null)
                .joinToken(UUID.randomUUID().toString().replace("-", ""))
                .build();

        PujaBooking savedBooking = bookingRepo.save(booking);
        PujaBookingSpiritualDetail savedSpiritualDetail = pujaBookingSpiritualDetailRepository.save(
                PujaBookingSpiritualDetail.builder()
                        .booking(savedBooking)
                        .userId(userId)
                        .gotraMasterId(gotraMaster.getId())
                        .rashiMasterId(rashiMaster == null ? null : rashiMaster.getId())
                        .nakshatraMasterId(nakshatraMaster == null ? null : nakshatraMaster.getId())
                        .gotraName(resolvedGotraName)
                        .rashiName(rashiMaster == null ? null : rashiMaster.getName())
                        .nakshatraName(nakshatraMaster == null ? null : nakshatraMaster.getName())
                        .build()
        );
        persistUserSpiritualPreference(
                userId,
                savedSpiritualDetail.getGotraMasterId(),
                savedSpiritualDetail.getRashiMasterId(),
                savedSpiritualDetail.getNakshatraMasterId()
        );
        orderHistoryService.recordPujaBooking(savedBooking, puja, slot);
        try {
            pujaBookingNotificationService.notifyAdminAndPandit(savedBooking, puja, slot);
        } catch (Exception ignored) {
        }
        if (isPaymentConfirmed(savedBooking) && Boolean.TRUE.equals(savedBooking.getSlotSelectedByMobile())) {
            sendPujaReceiptIfEmailAvailable(savedBooking, puja, slot);
            sendCalendarInviteIfEmailAvailable(savedBooking, false);
        }
        return savedBooking;
    }

    public Map<String, Object> getBookingMasters() {
        return Map.of(
                "status", true,
                "gotra", gotraMasterRepository.findByIsActiveOrderByName(true),
                "rashi", rashiMasterRepository.findByIsActiveOrderByName(true),
                "nakshatra", nakshatraMasterRepository.findByIsActiveOrderByName(true),
                "mobileSlotSelectionEnabled", isMobileSlotSelectionEnabled()
        );
    }

    public Map<String, Object> getUserBookingPreferences(Long userId) {
        PujaBookingSpiritualDetail latest = pujaBookingSpiritualDetailRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId).orElse(null);

        Long gotraMasterId = latest == null ? null : latest.getGotraMasterId();
        Long rashiMasterId = latest == null ? null : latest.getRashiMasterId();
        Long nakshatraMasterId = latest == null ? null : latest.getNakshatraMasterId();
        String gotraName = latest == null ? null : latest.getGotraName();
        String rashiName = latest == null ? null : latest.getRashiName();
        String nakshatraName = latest == null ? null : latest.getNakshatraName();

        if (profile != null) {
            if (gotraMasterId == null || gotraMasterId <= 0) {
                gotraMasterId = profile.getGotraMasterId();
            }
            if (rashiMasterId == null || rashiMasterId <= 0) {
                rashiMasterId = profile.getRashiMasterId();
            }
            if (nakshatraMasterId == null || nakshatraMasterId <= 0) {
                nakshatraMasterId = profile.getNakshatraMasterId();
            }
        }

        if ((gotraName == null || gotraName.isBlank()) && gotraMasterId != null && gotraMasterId > 0) {
            gotraName = gotraMasterRepository.findById(gotraMasterId).map(GotraMaster::getName).orElse(null);
        }
        if ((rashiName == null || rashiName.isBlank()) && rashiMasterId != null && rashiMasterId > 0) {
            rashiName = rashiMasterRepository.findById(rashiMasterId).map(RashiMaster::getName).orElse(null);
        }
        if ((nakshatraName == null || nakshatraName.isBlank()) && nakshatraMasterId != null && nakshatraMasterId > 0) {
            nakshatraName = nakshatraMasterRepository.findById(nakshatraMasterId).map(NakshatraMaster::getName).orElse(null);
        }

        if ((gotraMasterId == null || gotraMasterId <= 0)
                && (rashiMasterId == null || rashiMasterId <= 0)
                && (nakshatraMasterId == null || nakshatraMasterId <= 0)) {
            return Map.of(
                    "status", true,
                    "message", "No previous spiritual details found",
                    "preferences", Map.of()
            );
        }

        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("gotraMasterId", gotraMasterId);
        preferences.put("gotraName", gotraName);
        preferences.put("rashiMasterId", rashiMasterId);
        preferences.put("rashiName", rashiName);
        preferences.put("nakshatraMasterId", nakshatraMasterId);
        preferences.put("nakshatraName", nakshatraName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("preferences", preferences);
        return response;
    }

    public Map<String, Object> updateUserBookingPreferences(
            Long userId,
            Long gotraMasterId,
            String customGotraName,
            Long rashiMasterId,
            Long nakshatraMasterId
    ) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (gotraMasterId == null || gotraMasterId <= 0) {
            throw new RuntimeException("gotraMasterId is required");
        }

        GotraMaster gotraMaster = gotraMasterRepository.findById(gotraMasterId)
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .orElseThrow(() -> new RuntimeException("Invalid gotra selected"));
        final String resolvedGotraName = resolveGotraDisplayName(gotraMaster, customGotraName);

        RashiMaster rashiMaster = null;
        if (rashiMasterId != null && rashiMasterId > 0) {
            rashiMaster = rashiMasterRepository.findById(rashiMasterId)
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .orElseThrow(() -> new RuntimeException("Invalid rashi selected"));
        }

        NakshatraMaster nakshatraMaster = null;
        if (nakshatraMasterId != null && nakshatraMasterId > 0) {
            nakshatraMaster = nakshatraMasterRepository.findById(nakshatraMasterId)
                    .filter(n -> Boolean.TRUE.equals(n.getIsActive()))
                    .orElseThrow(() -> new RuntimeException("Invalid nakshatra selected"));
        }

        boolean preferencePersisted = persistUserSpiritualPreference(
                userId,
                gotraMaster.getId(),
                rashiMaster == null ? null : rashiMaster.getId(),
                nakshatraMaster == null ? null : nakshatraMaster.getId()
        );

        PujaBookingSpiritualDetail latest = pujaBookingSpiritualDetailRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (latest != null) {
            latest.setGotraMasterId(gotraMaster.getId());
            latest.setGotraName(resolvedGotraName);
            latest.setRashiMasterId(rashiMaster == null ? null : rashiMaster.getId());
            latest.setRashiName(rashiMaster == null ? null : rashiMaster.getName());
            latest.setNakshatraMasterId(nakshatraMaster == null ? null : nakshatraMaster.getId());
            latest.setNakshatraName(nakshatraMaster == null ? null : nakshatraMaster.getName());
            latest.setIsActive(true);
            pujaBookingSpiritualDetailRepository.save(latest);
        }

        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("gotraMasterId", gotraMaster.getId());
        preferences.put("gotraName", resolvedGotraName);
        preferences.put("rashiMasterId", rashiMaster == null ? null : rashiMaster.getId());
        preferences.put("rashiName", rashiMaster == null ? null : rashiMaster.getName());
        preferences.put("nakshatraMasterId", nakshatraMaster == null ? null : nakshatraMaster.getId());
        preferences.put("nakshatraName", nakshatraMaster == null ? null : nakshatraMaster.getName());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put(
                "message",
                (latest != null || preferencePersisted)
                        ? "Preferences updated successfully."
                        : "No profile found. Preference will be saved at booking time."
        );
        response.put("preferences", preferences);
        return response;
    }

    public Map<String, Object> updateBookingSpiritualDetails(
            Long bookingId,
            Long gotraMasterId,
            String customGotraName,
            Long rashiMasterId,
            Long nakshatraMasterId
    ) {
        if (bookingId == null || bookingId <= 0) {
            throw new RuntimeException("Valid bookingId is required");
        }

        PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));

        PujaBookingSpiritualDetail detail = pujaBookingSpiritualDetailRepository
                .findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElse(null);

        Long resolvedGotraId = gotraMasterId != null && gotraMasterId > 0
                ? gotraMasterId
                : (detail == null ? null : detail.getGotraMasterId());
        if (resolvedGotraId == null || resolvedGotraId <= 0) {
            throw new RuntimeException("gotraMasterId is required");
        }

        GotraMaster gotraMaster = gotraMasterRepository.findById(resolvedGotraId)
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .orElseThrow(() -> new RuntimeException("Invalid gotra selected"));
        final String resolvedGotraName = resolveGotraDisplayName(gotraMaster, customGotraName);

        RashiMaster rashiMaster = null;
        if (rashiMasterId != null && rashiMasterId > 0) {
            rashiMaster = rashiMasterRepository.findById(rashiMasterId)
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .orElseThrow(() -> new RuntimeException("Invalid rashi selected"));
        }

        NakshatraMaster nakshatraMaster = null;
        if (nakshatraMasterId != null && nakshatraMasterId > 0) {
            nakshatraMaster = nakshatraMasterRepository.findById(nakshatraMasterId)
                    .filter(n -> Boolean.TRUE.equals(n.getIsActive()))
                    .orElseThrow(() -> new RuntimeException("Invalid nakshatra selected"));
        }

        PujaBookingSpiritualDetail entity = detail == null
                ? PujaBookingSpiritualDetail.builder()
                .booking(booking)
                .userId(booking.getUserId())
                .isActive(true)
                .build()
                : detail;

        entity.setGotraMasterId(gotraMaster.getId());
        entity.setGotraName(resolvedGotraName);
        entity.setRashiMasterId(rashiMaster == null ? null : rashiMaster.getId());
        entity.setRashiName(rashiMaster == null ? null : rashiMaster.getName());
        entity.setNakshatraMasterId(nakshatraMaster == null ? null : nakshatraMaster.getId());
        entity.setNakshatraName(nakshatraMaster == null ? null : nakshatraMaster.getName());
        entity.setIsActive(true);

        PujaBookingSpiritualDetail saved = pujaBookingSpiritualDetailRepository.save(entity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Booking spiritual details updated");
        response.put("bookingId", bookingId);
        response.put("gotraMasterId", saved.getGotraMasterId());
        response.put("rashiMasterId", saved.getRashiMasterId());
        response.put("nakshatraMasterId", saved.getNakshatraMasterId());
        response.put("inviteQueued", false);
        return response;
    }

    private boolean persistUserSpiritualPreference(
            Long userId,
            Long gotraMasterId,
            Long rashiMasterId,
            Long nakshatraMasterId
    ) {
        if (userId == null || userId <= 0) {
            return false;
        }
        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        profile.setGotraMasterId(gotraMasterId);
        profile.setRashiMasterId(rashiMasterId);
        profile.setNakshatraMasterId(nakshatraMasterId);
        mobileUserProfileRepository.save(profile);
        return true;
    }

    public List<PujaSamagriMaster> getAllSamagriMasterForAdmin() {
        return pujaSamagriMasterRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    String an = a.getName() == null ? "" : a.getName();
                    String bn = b.getName() == null ? "" : b.getName();
                    return an.compareToIgnoreCase(bn);
                })
                .toList();
    }

    public PujaSamagriMaster createSamagriMaster(PujaSamagriMasterRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        String cleanedName = request.getName() == null ? "" : request.getName().trim();
        if (cleanedName.isEmpty()) {
            throw new RuntimeException("Samagri name is required");
        }
        String cleanedHiName = request.getHiName() == null ? "" : request.getHiName().trim();
        if (cleanedHiName.isEmpty()) {
            cleanedHiName = cleanedName;
        }

        final List<String> normalizedImages = normalizeImageUrls(request.getImageUrl(), request.getImageUrls());

        PujaSamagriMaster entity = PujaSamagriMaster.builder()
                .name(cleanedName)
                .hiName(cleanedHiName)
                .description(request.getDescription() == null ? null : request.getDescription().trim())
                .hiDescription(request.getHiDescription() == null ? null : request.getHiDescription().trim())
                .price(request.getPrice())
                .discountPercentage(request.getDiscountPercentage())
                .currency(request.getCurrency() == null ? null : request.getCurrency().trim())
                .imageUrl(normalizedImages.isEmpty() ? trimToNull(request.getImageUrl()) : normalizedImages.get(0))
                .shopEnabled(request.getShopEnabled() != null ? request.getShopEnabled() : false)
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .build();

        PujaSamagriMaster saved = pujaSamagriMasterRepository.save(entity);
        syncSamagriMasterImages(saved, normalizedImages);
        return saved;
    }

    public PujaSamagriMaster updateSamagriMaster(Long id, PujaSamagriMasterRequest request) {
        PujaSamagriMaster entity = pujaSamagriMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Samagri master not found: " + id));

        boolean replaceImages = false;
        List<String> normalizedImages = List.of();

        if (request != null) {
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                entity.setName(request.getName().trim());
            }
            if (request.getHiName() != null) {
                final String hi = request.getHiName().trim();
                if (!hi.isEmpty()) {
                    entity.setHiName(hi);
                }
            }
            if (request.getDescription() != null) {
                entity.setDescription(request.getDescription().trim());
            }
            if (request.getHiDescription() != null) {
                entity.setHiDescription(request.getHiDescription().trim());
            }
            if (request.getPrice() != null) {
                entity.setPrice(request.getPrice());
            }
            if (request.getDiscountPercentage() != null) {
                entity.setDiscountPercentage(request.getDiscountPercentage());
            }
            if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
                entity.setCurrency(request.getCurrency().trim());
            }
            replaceImages = request.getImageUrls() != null;
            if (replaceImages) {
                normalizedImages = normalizeImageUrls(request.getImageUrl(), request.getImageUrls());
                if (!normalizedImages.isEmpty()) {
                    entity.setImageUrl(normalizedImages.get(0));
                } else if (request.getImageUrl() != null) {
                    entity.setImageUrl(trimToNull(request.getImageUrl()));
                }
            } else if (request.getImageUrl() != null) {
                entity.setImageUrl(trimToNull(request.getImageUrl()));
            }
            if (request.getShopEnabled() != null) {
                entity.setShopEnabled(request.getShopEnabled());
            }
            if (request.getIsActive() != null) {
                entity.setIsActive(request.getIsActive());
            }
        }

        if (entity.getHiName() == null || entity.getHiName().trim().isEmpty()) {
            entity.setHiName(entity.getName());
        }

        PujaSamagriMaster saved = pujaSamagriMasterRepository.save(entity);

        if (replaceImages) {
            syncSamagriMasterImages(saved, normalizedImages);
        } else if (request != null && request.getImageUrl() != null) {
            upsertPrimarySamagriImage(saved, saved.getImageUrl());
        }

        return saved;
    }

public void softDeleteSamagriMaster(Long id) {
        PujaSamagriMaster entity = pujaSamagriMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Samagri master not found: " + id));
        entity.setIsActive(false);
        pujaSamagriMasterRepository.save(entity);
    }

    private void syncSamagriMasterImages(PujaSamagriMaster master, List<String> imageUrls) {
        if (master == null || master.getId() == null) {
            return;
        }

        List<PujaSamagriMasterImage> existing = pujaSamagriMasterImageRepository
                .findBySamagriMaster_IdOrderByDisplayOrderAscIdAsc(master.getId());
        if (!existing.isEmpty()) {
            existing.forEach(img -> img.setIsActive(false));
            pujaSamagriMasterImageRepository.saveAll(existing);
        }

        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<PujaSamagriMasterImage> toSave = new ArrayList<>();
        int order = 0;
        for (String url : imageUrls) {
            if (url == null || url.trim().isEmpty()) continue;
            toSave.add(PujaSamagriMasterImage.builder()
                    .samagriMaster(master)
                    .imageUrl(url.trim())
                    .displayOrder(order++)
                    .isActive(true)
                    .build());
        }
        if (!toSave.isEmpty()) {
            pujaSamagriMasterImageRepository.saveAll(toSave);
        }
    }

    private void upsertPrimarySamagriImage(PujaSamagriMaster master, String imageUrl) {
        if (master == null || master.getId() == null) {
            return;
        }
        final String cleaned = trimToNull(imageUrl);
        if (cleaned == null) {
            return;
        }

        List<PujaSamagriMasterImage> active = pujaSamagriMasterImageRepository
                .findBySamagriMaster_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(master.getId());
        if (active.isEmpty()) {
            pujaSamagriMasterImageRepository.save(PujaSamagriMasterImage.builder()
                    .samagriMaster(master)
                    .imageUrl(cleaned)
                    .displayOrder(0)
                    .isActive(true)
                    .build());
            return;
        }

        PujaSamagriMasterImage first = active.get(0);
        first.setImageUrl(cleaned);
        first.setIsActive(true);
        if (first.getDisplayOrder() == null) {
            first.setDisplayOrder(0);
        }
        pujaSamagriMasterImageRepository.save(first);
    }

    private List<String> normalizeImageUrls(String primary, List<String> imageUrls) {
        final LinkedHashSet<String> ordered = new LinkedHashSet<>();

        final String p = trimToNull(primary);
        if (p != null) {
            ordered.add(p);
        }

        if (imageUrls != null) {
            for (String url : imageUrls) {
                final String cleaned = trimToNull(url);
                if (cleaned == null) continue;
                ordered.add(cleaned);
                if (ordered.size() >= 12) break;
            }
        }

        return ordered.stream().toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        final String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public List<Map<String, Object>> getPujaSamagriForMobile(Long pujaId) {
        List<PujaSamagriItem> items = pujaSamagriItemRepository
                .findByPujaIdAndIsActiveOrderByDisplayOrderAscIdAsc(pujaId, true);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, PujaSamagriMaster> masterById = pujaSamagriMasterRepository.findAllById(
                items.stream().map(PujaSamagriItem::getSamagriMasterId).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(PujaSamagriMaster::getId, m -> m));

        List<Map<String, Object>> response = new ArrayList<>();
        for (PujaSamagriItem item : items) {
            PujaSamagriMaster master = masterById.get(item.getSamagriMasterId());
            if (master == null || Boolean.FALSE.equals(master.getIsActive())) {
                continue;
            }
            response.add(Map.of(
                    "itemId", item.getId(),
                    "pujaId", item.getPujaId(),
                    "samagriMasterId", master.getId(),
                    "samagriName", master.getName(),
                    "quantity", item.getQuantity() == null ? "" : item.getQuantity(),
                    "displayOrder", item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()
            ));
        }
        return response;
    }

    public List<Map<String, Object>> getPujaSamagriForAdmin(Long pujaId) {
        List<PujaSamagriItem> items = pujaSamagriItemRepository
                .findByPujaIdAndIsActiveOrderByDisplayOrderAscIdAsc(pujaId, true);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, PujaSamagriMaster> masterById = pujaSamagriMasterRepository.findAllById(
                items.stream().map(PujaSamagriItem::getSamagriMasterId).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(PujaSamagriMaster::getId, m -> m));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (PujaSamagriItem item : items) {
            PujaSamagriMaster master = masterById.get(item.getSamagriMasterId());
            rows.add(Map.of(
                    "id", item.getId(),
                    "pujaId", item.getPujaId(),
                    "samagriMasterId", item.getSamagriMasterId(),
                    "samagriName", master == null ? "" : master.getName(),
                    "quantity", item.getQuantity() == null ? "" : item.getQuantity(),
                    "displayOrder", item.getDisplayOrder() == null ? 0 : item.getDisplayOrder(),
                    "isActive", item.getIsActive()
            ));
        }
        return rows;
    }

    public Map<String, Object> addSamagriToPuja(
            Long pujaId,
            Long bookingId,
            Long samagriMasterId,
            String quantity,
            String unit,
            String notes,
            Integer displayOrder
    ) {
        pujaRepo.findById(pujaId).orElseThrow(() -> new RuntimeException("Puja not found: " + pujaId));
        if (bookingId != null && bookingId > 0) {
            PujaBooking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
            if (!Objects.equals(booking.getPujaId(), pujaId)) {
                throw new RuntimeException("Booking does not belong to selected puja.");
            }
            if (!isSamagriRequiredForBooking(booking)) {
                throw new RuntimeException("Samagri can only be added for BASIC package bookings.");
            }
        }
        PujaSamagriMaster master = pujaSamagriMasterRepository.findById(samagriMasterId)
                .orElseThrow(() -> new RuntimeException("Samagri master not found: " + samagriMasterId));
        if (Boolean.FALSE.equals(master.getIsActive())) {
            throw new RuntimeException("Selected samagri is inactive");
        }

        PujaSamagriItem item = pujaSamagriItemRepository
                .findByPujaIdAndSamagriMasterIdAndIsActive(pujaId, samagriMasterId, true)
                .orElse(PujaSamagriItem.builder()
                        .pujaId(pujaId)
                        .samagriMasterId(samagriMasterId)
                        .build());
        item.setQuantity(quantity == null ? null : quantity.trim());
        item.setUnit(unit == null ? null : unit.trim());
        item.setNotes(notes == null ? null : notes.trim());
        item.setDisplayOrder(displayOrder == null ? 0 : displayOrder);
        item.setIsActive(true);
        PujaSamagriItem saved = pujaSamagriItemRepository.save(item);
        return Map.of(
                "status", true,
                "message", "Puja samagri saved",
                "itemId", saved.getId(),
                "pujaId", saved.getPujaId(),
                "bookingId", bookingId,
                "samagriMasterId", saved.getSamagriMasterId(),
                "samagriMailQueued", 0
        );
    }

    public Map<String, Object> updatePujaSamagriItem(
            Long itemId,
            String quantity,
            String unit,
            String notes,
            Integer displayOrder,
            Boolean isActive
    ) {
        PujaSamagriItem item = pujaSamagriItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Puja samagri item not found: " + itemId));
        if (quantity != null) {
            item.setQuantity(quantity.trim());
        }
        if (unit != null) {
            item.setUnit(unit.trim());
        }
        if (notes != null) {
            item.setNotes(notes.trim());
        }
        if (displayOrder != null) {
            item.setDisplayOrder(displayOrder);
        }
        if (isActive != null) {
            item.setIsActive(isActive);
        }
        PujaSamagriItem saved = pujaSamagriItemRepository.save(item);
        return Map.of(
                "status", true,
                "message", "Puja samagri item updated",
                "itemId", saved.getId(),
                "samagriMailQueued", 0
        );
    }

    public void deletePujaSamagriItem(Long itemId) {
        PujaSamagriItem item = pujaSamagriItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Puja samagri item not found: " + itemId));
        item.setIsActive(false);
        pujaSamagriItemRepository.save(item);
    }

    public Map<String, Object> generateSlotsFromMaster(PujaSlotMasterRequest request) {
        if (request.getPujaId() == null) {
            throw new RuntimeException("pujaId is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("startDate and endDate are required in YYYY-MM-DD");
        }
        final Puja puja = pujaRepo.findById(request.getPujaId())
                .orElseThrow(() -> new RuntimeException("Invalid pujaId"));

        final int durationMinutes = puja.getDurationMinutes();
        if (durationMinutes <= 0) {
            throw new RuntimeException("Puja durationMinutes must be greater than 0");
        }

        final int gapMinutes = DEFAULT_GAP_MINUTES;
        if (gapMinutes < 0) {
            throw new RuntimeException("gapMinutes cannot be negative");
        }

        final int maxBookings = request.getMaxBookings() == null || request.getMaxBookings() <= 0
                ? 1
                : request.getMaxBookings();

        final LocalDate startDate;
        final LocalDate endDate;
        final LocalTime dayStart;
        final LocalTime dayEnd;
        try {
            startDate = LocalDate.parse(request.getStartDate().trim());
            endDate = LocalDate.parse(request.getEndDate().trim());
            dayStart = DEFAULT_DAY_START;
            dayEnd = DEFAULT_DAY_END;
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date/time format. Use startDate/endDate=YYYY-MM-DD and dayStartTime/dayEndTime=HH:mm");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("endDate must be on or after startDate");
        }
        if (!dayEnd.isAfter(dayStart)) {
            throw new RuntimeException("dayEndTime must be after dayStartTime");
        }

        long createdCount = 0;
        long duplicateCount = 0;
        long tooShortWindowDays = 0;
        List<PujaSlot> toSave = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalDateTime cursor = d.atTime(dayStart);
            final LocalDateTime dayClose = d.atTime(dayEnd);

            if (cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                tooShortWindowDays++;
                continue;
            }

            while (!cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                if (slotRepo.existsByPujaIdAndSlotTime(request.getPujaId(), cursor)) {
                    duplicateCount++;
                } else {
                    toSave.add(
                            PujaSlot.builder()
                                    .pujaId(request.getPujaId())
                                    .slotTime(cursor)
                                    .status(PujaSlot.SlotStatus.AVAILABLE)
                                    .astrologerId(request.getAstrologerId())
                                    .maxBookings(maxBookings)
                                    .currentBookings(0)
                                    .isRecurring(Boolean.TRUE.equals(request.getIsRecurring()))
                                    .recurringPattern(request.getRecurringPattern())
                                    .isActive(true)
                                    .build()
                    );
                    createdCount++;
                }
                cursor = cursor.plusMinutes((long) durationMinutes + gapMinutes);
            }
        }

        if (!toSave.isEmpty()) {
            slotRepo.saveAll(toSave);
        }

        return Map.of(
                "status", true,
                "message", "Puja slots generated successfully",
                "pujaId", request.getPujaId(),
                "pujaDurationMinutes", durationMinutes,
                "gapMinutes", gapMinutes,
                "createdSlots", createdCount,
                "duplicateSlotsSkipped", duplicateCount,
                "daysWithNoWindow", tooShortWindowDays,
                "fromDate", startDate.toString(),
                "toDate", endDate.toString()
        );
    }

    public void ensureDefaultSlotsForPuja(Long pujaId) {
        if (pujaId == null || pujaId <= 0) {
            return;
        }
        final Puja puja = pujaRepo.findById(pujaId).orElse(null);
        if (puja == null) {
            return;
        }
        final int durationMinutes = puja.getDurationMinutes() <= 0 ? 60 : puja.getDurationMinutes();

        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = startDate.plusDays(30);
        final int maxBookings = 1;

        List<PujaSlot> toSave = new ArrayList<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            LocalDateTime cursor = day.atTime(DEFAULT_DAY_START);
            final LocalDateTime dayClose = day.atTime(DEFAULT_DAY_END);

            while (!cursor.plusMinutes(durationMinutes).isAfter(dayClose)) {
                if (!slotRepo.existsByPujaIdAndSlotTime(pujaId, cursor)) {
                    toSave.add(PujaSlot.builder()
                            .pujaId(pujaId)
                            .slotTime(cursor)
                            .status(PujaSlot.SlotStatus.AVAILABLE)
                            .maxBookings(maxBookings)
                            .currentBookings(0)
                            .isRecurring(false)
                            .recurringPattern(null)
                            .isActive(true)
                            .build());
                }
                cursor = cursor.plusMinutes((long) durationMinutes + DEFAULT_GAP_MINUTES);
            }
        }
        if (!toSave.isEmpty()) {
            slotRepo.saveAll(toSave);
        }
    }

    public List<PujaRescheduleItemResponse> getUserRescheduleBookings(Long userId) {
        List<PujaBooking> bookings = bookingRepo.findByUserIdOrderByBookedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        return bookings.stream()
                .map(booking -> {
                    Puja puja = booking.getPujaId() == null
                            ? null
                            : pujaRepo.findById(booking.getPujaId()).orElse(null);
                    PujaSlot currentSlot = booking.getSlotId() == null
                            ? null
                            : slotRepo.findById(booking.getSlotId()).orElse(null);

                    boolean statusEligible = booking.getStatus() == PujaBooking.BookingStatus.CONFIRMED
                            || booking.getStatus() == PujaBooking.BookingStatus.PENDING;

                    LocalDateTime cutoff = currentSlot != null && currentSlot.getSlotTime() != null
                            ? currentSlot.getSlotTime().minusDays(1)
                            : null;

                    boolean timeEligible = cutoff != null && !now.isAfter(cutoff);
                    boolean canReschedule = statusEligible && timeEligible;

                    String msg;
                    if (!statusEligible) {
                        msg = "Reschedule allowed only for confirmed/pending bookings.";
                    } else if (currentSlot == null || currentSlot.getSlotTime() == null) {
                        msg = "Current slot details unavailable.";
                    } else if (!timeEligible) {
                        msg = "Rescheduling window closed (allowed until 1 day before slot).";
                    } else {
                        msg = "Eligible for rescheduling.";
                    }

                    return PujaRescheduleItemResponse.builder()
                            .bookingId(booking.getId())
                            .pujaId(booking.getPujaId())
                            .pujaName(puja != null ? puja.getName() : "Puja")
                            .currentSlotId(booking.getSlotId())
                            .currentSlotTime(currentSlot != null ? currentSlot.getSlotTime() : null)
                            .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                            .totalPrice(booking.getTotalPrice())
                            .canReschedule(canReschedule)
                            .rescheduleAllowedTill(cutoff)
                            .rescheduleMessage(msg)
                            .build();
                })
                .toList();
    }

    public Map<String, Object> getTodayBookingSegregationForAdmin() {
        LocalDate today = LocalDate.now();
        List<PujaBooking> bookings = bookingRepo.findAll()
                .stream()
                .sorted((a, b) -> {
                    LocalDateTime aa = a.getBookedAt();
                    LocalDateTime bb = b.getBookedAt();
                    if (aa == null && bb == null) return 0;
                    if (aa == null) return 1;
                    if (bb == null) return -1;
                    return bb.compareTo(aa);
                })
                .toList();
        if (bookings.isEmpty()) {
            return Map.of(
                    "status", true,
                    "date", today.toString(),
                    "scope", "ALL_BOOKINGS",
                    "pending", List.of(),
                    "completed", List.of(),
                    "pendingCount", 0,
                    "completedCount", 0
            );
        }

        List<Map<String, Object>> pending = new ArrayList<>();
        List<Map<String, Object>> completed = new ArrayList<>();

        for (PujaBooking booking : bookings) {
            Puja puja = booking.getPujaId() == null ? null : pujaRepo.findById(booking.getPujaId()).orElse(null);
            PujaSlot slot = booking.getSlotId() == null ? null : slotRepo.findById(booking.getSlotId()).orElse(null);
            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
            PujaBookingSpiritualDetail spiritual = pujaBookingSpiritualDetailRepository
                    .findTopByBookingIdOrderByCreatedAtDesc(booking.getId() == null ? 0L : booking.getId())
                    .orElse(null);

            boolean appSlotBookingEnabled = isPujaSlotSelectionEnabled(puja);
            String packageCode = resolveBookingPackageCode(booking);
            String packageName = resolveBookingPackageName(booking);
            boolean samagriRequired = isSamagriRequiredForBooking(booking);
            boolean hasPujaTime = slot != null && slot.getSlotTime() != null;
            boolean hasRashi = spiritual != null && spiritual.getRashiMasterId() != null;
            boolean hasNakshatra = spiritual != null && spiritual.getNakshatraMasterId() != null;
            boolean hasSamagri = !getPujaSamagriForAdmin(booking.getPujaId() == null ? 0L : booking.getPujaId()).isEmpty();
            boolean hasRequiredSamagri = !samagriRequired || hasSamagri;
            boolean isCompleted = appSlotBookingEnabled
                    ? hasRequiredSamagri
                    : (hasPujaTime && hasRashi && hasNakshatra && hasRequiredSamagri);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId", booking.getId());
            row.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), booking.getId()));
            row.put("userId", booking.getUserId());
            row.put("userName", profile == null || profile.getName() == null ? "Unknown" : profile.getName());
            row.put("email", profile == null || profile.getEmail() == null ? "" : profile.getEmail());
            row.put("pujaId", booking.getPujaId());
            row.put("pujaName", puja == null || puja.getName() == null ? "Puja" : puja.getName());
            row.put("isSlot", appSlotBookingEnabled);
            row.put("slotId", booking.getSlotId());
            row.put("slotTime", slot == null ? null : slot.getSlotTime());
            row.put("bookedAt", booking.getBookedAt());
            row.put("bookingStatus", booking.getStatus());
            row.put("packageCode", packageCode);
            row.put("packageName", packageName);
            row.put("packagePrice", booking.getPackagePrice());
            row.put("gotraMasterId", spiritual == null ? null : spiritual.getGotraMasterId());
            row.put("gotraName", spiritual == null ? null : spiritual.getGotraName());
            row.put("rashiMasterId", spiritual == null ? null : spiritual.getRashiMasterId());
            row.put("rashiName", spiritual == null ? null : spiritual.getRashiName());
            row.put("nakshatraMasterId", spiritual == null ? null : spiritual.getNakshatraMasterId());
            row.put("nakshatraName", spiritual == null ? null : spiritual.getNakshatraName());
            row.put("hasPujaTime", hasPujaTime);
            row.put("hasRashi", hasRashi);
            row.put("hasNakshatra", hasNakshatra);
            row.put("hasSamagri", hasSamagri);
            row.put("hasRequiredSamagri", hasRequiredSamagri);
            row.put("canAdminAddSamagri", samagriRequired);
            row.put("samagriMailEnabled", samagriRequired);
            row.put("isCompleted", isCompleted);

            if (isCompleted) {
                completed.add(row);
            } else {
                List<String> missing = new ArrayList<>();
                if (!appSlotBookingEnabled && !hasPujaTime) missing.add("pujaTime");
                if (!appSlotBookingEnabled && !hasRashi) missing.add("rashi");
                if (!appSlotBookingEnabled && !hasNakshatra) missing.add("nakshatra");
                if (samagriRequired && !hasSamagri) missing.add("samagri");
                row.put("missing", missing);
                pending.add(row);
            }
        }

        return Map.of(
                "status", true,
                "date", today.toString(),
                "scope", "ALL_BOOKINGS",
                "pending", pending,
                "completed", completed,
                "pendingCount", pending.size(),
                "completedCount", completed.size()
        );
    }

    public Map<String, Object> assignBookingSlotByAdmin(Long bookingId, Long slotId) {
        if (bookingId == null || bookingId <= 0) {
            throw new RuntimeException("Valid bookingId is required");
        }
        if (slotId == null || slotId <= 0) {
            throw new RuntimeException("Valid slotId is required");
        }

        PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
        Puja puja = booking.getPujaId() == null ? null : pujaRepo.findById(booking.getPujaId()).orElse(null);
        if (isPujaSlotSelectionEnabled(puja)) {
            throw new RuntimeException("This puja uses app-side slot booking. Admin slot assignment is not allowed.");
        }
        PujaSlot newSlot = slotRepo.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));

        if (booking.getPujaId() == null || !Objects.equals(newSlot.getPujaId(), booking.getPujaId())) {
            throw new RuntimeException("Selected slot does not belong to this puja booking.");
        }
        if (Boolean.FALSE.equals(newSlot.getIsActive())) {
            throw new RuntimeException("Selected slot is not active.");
        }
        if (newSlot.getSlotTime() == null || !newSlot.getSlotTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Selected slot is expired. Please choose another slot.");
        }
        if (!Objects.equals(booking.getSlotId(), newSlot.getId())
                && newSlot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
            throw new RuntimeException("Selected slot is not available.");
        }

        if (booking.getSlotId() != null && !Objects.equals(booking.getSlotId(), newSlot.getId())) {
            PujaSlot oldSlot = slotRepo.findById(booking.getSlotId()).orElse(null);
            if (oldSlot != null && oldSlot.getStatus() == PujaSlot.SlotStatus.BOOKED) {
                oldSlot.setStatus(PujaSlot.SlotStatus.AVAILABLE);
                slotRepo.save(oldSlot);
            }
        }

        newSlot.setStatus(PujaSlot.SlotStatus.BOOKED);
        slotRepo.save(newSlot);

        booking.setSlotId(newSlot.getId());
        bookingRepo.save(booking);
        try {
            pujaBookingNotificationService.notifyAdminAndPandit(booking, puja, newSlot);
        } catch (Exception ignored) {
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Booking slot assigned by admin");
        response.put("bookingId", booking.getId());
        response.put("slotId", newSlot.getId());
        response.put("slotTime", newSlot.getSlotTime());
        return response;
    }

    @Transactional
    public Map<String, Object> finalizeBookingByAdmin(Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw new RuntimeException("Valid bookingId is required");
        }

        PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + bookingId));
        Puja puja = booking.getPujaId() == null ? null : pujaRepo.findById(booking.getPujaId()).orElse(null);
        boolean appSlotBookingEnabled = isPujaSlotSelectionEnabled(puja);
        PujaSlot slot = booking.getSlotId() == null ? null : slotRepo.findById(booking.getSlotId()).orElse(null);
        PujaBookingSpiritualDetail spiritual = pujaBookingSpiritualDetailRepository
                .findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElse(null);
        boolean samagriRequired = isSamagriRequiredForBooking(booking);
        List<Map<String, Object>> samagri = getPujaSamagriForAdmin(booking.getPujaId() == null ? 0L : booking.getPujaId());

        if (samagriRequired && samagri.isEmpty()) {
            throw new RuntimeException("Please add at least one samagri item before confirmation.");
        }
        if (!appSlotBookingEnabled) {
            if (slot == null || slot.getSlotTime() == null) {
                throw new RuntimeException("Please set valid puja slot time before confirmation.");
            }
            if (spiritual == null || spiritual.getGotraMasterId() == null || spiritual.getGotraMasterId() <= 0) {
                throw new RuntimeException("Please update gotra before confirmation.");
            }
            if (spiritual.getRashiMasterId() == null || spiritual.getRashiMasterId() <= 0) {
                throw new RuntimeException("Please update rashi before confirmation.");
            }
            if (spiritual.getNakshatraMasterId() == null || spiritual.getNakshatraMasterId() <= 0) {
                throw new RuntimeException("Please update nakshatra before confirmation.");
            }
        }

        boolean inviteQueued = appSlotBookingEnabled
                ? false
                : (Boolean.TRUE.equals(booking.getSlotSelectedByMobile())
                ? false
                : sendCalendarInviteIfEmailAvailable(booking));
        boolean samagriQueued = samagriRequired && sendSamagriMailForBooking(booking);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        String message;
        if (samagriRequired) {
            message = appSlotBookingEnabled
                    ? "Samagri template sent for app-slot booking"
                    : "Booking finalized and emails queued";
        } else {
            message = "Booking finalized. Samagri is not required for selected package.";
        }
        response.put("message", message);
        response.put("bookingId", bookingId);
        response.put("inviteQueued", appSlotBookingEnabled ? false : inviteQueued);
        response.put("samagriMailQueued", samagriQueued ? 1 : 0);
        response.put("samagriRequired", samagriRequired);
        response.put("isSlot", appSlotBookingEnabled);
        return response;
    }

    public Map<String, Object> resendReceiptEmail(String orderId, ResendReceiptRequest request) {
        if (orderId == null || orderId.isBlank()) {
            throw new RuntimeException("Valid orderId is required");
        }
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final String toEmail = getVerifiedProfileEmail(request.getUserId(), request.getEmail());

        final Long bookingId = parseBookingId(orderId);
        final PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + orderId));
        if (!Objects.equals(booking.getUserId(), request.getUserId())) {
            throw new RuntimeException("Order does not belong to this user");
        }

        final Puja puja = booking.getPujaId() == null
                ? null
                : pujaRepo.findById(booking.getPujaId()).orElse(null);
        final PujaSlot slot = booking.getSlotId() == null
                ? null
                : slotRepo.findById(booking.getSlotId()).orElse(null);

        final String subject = "Your Astrologer Puja Receipt - " + PujaOrderIdHelper.build(booking.getUserId(), bookingId);
        final String html = buildPujaReceiptHtml(booking, puja, slot);
        final byte[] pdf = buildPujaReceiptPdf(booking, puja, slot);
        final String fileName = "invoice.pdf";
        emailService.sendEmailWithAttachmentAsync(
                toEmail,
                subject,
                html,
                fileName,
                pdf,
                "application/pdf"
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Receipt mail request sent successfully. You will receive the email shortly.");
        response.put("queued", true);
        response.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
        response.put("email", toEmail);
        response.put("requestedAt", LocalDateTime.now());
        return response;
    }

    private String getVerifiedProfileEmail(Long userId, String requestedEmail) {
        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId).orElse(null);
        String profileEmail = profile == null || profile.getEmail() == null
                ? ""
                : profile.getEmail().trim();
        if (profileEmail.isEmpty()) {
            throw new RuntimeException("No email is linked with this account. Please update profile email first.");
        }

        String provided = requestedEmail == null ? "" : requestedEmail.trim();
        if (!provided.isEmpty() && !profileEmail.equalsIgnoreCase(provided)) {
            throw new RuntimeException("Email verification failed. Please use your registered profile email.");
        }
        return profileEmail;
    }

    private Long parseBookingId(String orderId) {
        try {
            return PujaOrderIdHelper.parseBookingId(orderId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid puja orderId: " + orderId);
        }
    }

    private String buildPujaReceiptHtml(PujaBooking booking, Puja puja, PujaSlot slot) {
        final String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                ? "Puja Booking"
                : puja.getName();
        final String bookedAt = formatDateTime(booking.getBookedAt());
        final String slotTime = formatDateTime(slot == null ? null : slot.getSlotTime());
        final String status = booking.getStatus() == null ? "-" : booking.getStatus().name();
        final String paymentMethod = booking.getPaymentMethod() == null || booking.getPaymentMethod().isBlank()
                ? "-"
                : booking.getPaymentMethod();
        final String txnId = booking.getTransactionId() == null || booking.getTransactionId().isBlank()
                ? "-"
                : booking.getTransactionId();
        final double amount = booking.getTotalPrice() == null ? 0.0 : booking.getTotalPrice();
        final String amountText = formatMoney(amount, "INR");

        return """
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { margin:0; padding:0; background:#f4f6fa; font-family:Arial,sans-serif; color:#1f2533; }
                    .wrap { width:100%%; padding:22px 14px; }
                    .card { max-width:680px; margin:0 auto; background:#ffffff; border:1px solid #e5e8ef; border-radius:14px; overflow:hidden; }
                    .head { padding:20px 22px; background:linear-gradient(90deg,#1f2f73,#3146a3); color:#fff; }
                    .head h1 { margin:0; font-size:20px; letter-spacing:0.2px; }
                    .head p { margin:8px 0 0 0; font-size:13px; color:#d8e0ff; }
                    .body { padding:20px 22px; }
                    .pill { display:inline-block; border-radius:999px; padding:6px 11px; font-size:12px; font-weight:700; background:#eef4ff; color:#26439a; }
                    table { width:100%%; border-collapse:collapse; margin-top:12px; }
                    th, td { padding:11px 8px; border-bottom:1px solid #edf0f5; font-size:13px; }
                    th { text-align:left; color:#586174; background:#f8faff; font-weight:700; }
                    td:last-child, th:last-child { text-align:right; }
                    .foot { margin-top:14px; font-size:12px; color:#6e7583; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="head">
                        <h1>Puja Booking Receipt</h1>
                        <p>Thank you for booking with Astrologer. Your invoice PDF is attached with this email.</p>
                      </div>
                      <div class="body">
                        <span class="pill">%s</span>
                        <table>
                          <thead>
                            <tr><th>Field</th><th>Value</th></tr>
                          </thead>
                          <tbody>
                            <tr><td>Order ID</td><td>%s</td></tr>
                            <tr><td>Puja Name</td><td>%s</td></tr>
                            <tr><td>Booked At</td><td>%s</td></tr>
                            <tr><td>Slot Time</td><td>%s</td></tr>
                            <tr><td>Payment Method</td><td>%s</td></tr>
                            <tr><td>Transaction ID</td><td>%s</td></tr>
                            <tr><td>Total Amount</td><td><strong>%s</strong></td></tr>
                          </tbody>
                        </table>
                        <div class="foot">For support, reply to this email. Keep this receipt for your records.</div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(status),
                escapeHtml(PujaOrderIdHelper.build(booking.getUserId(), booking.getId())),
                escapeHtml(pujaName),
                escapeHtml(bookedAt),
                escapeHtml(slotTime),
                escapeHtml(paymentMethod),
                escapeHtml(txnId),
                escapeHtml(amountText)
        );
    }

    private byte[] buildPujaReceiptPdf(PujaBooking booking, Puja puja, PujaSlot slot) {
        final String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                ? "Puja Booking"
                : puja.getName();
        final String orderId = PujaOrderIdHelper.build(booking.getUserId(), booking.getId());
        final String userId = booking.getUserId() == null ? "-" : String.valueOf(booking.getUserId());
        final String bookedAt = formatDateTime(booking.getBookedAt());
        final String slotTime = formatDateTime(slot == null ? null : slot.getSlotTime());
        final String paymentMethod = booking.getPaymentMethod() == null || booking.getPaymentMethod().isBlank()
                ? "-"
                : booking.getPaymentMethod();
        final String txnId = booking.getTransactionId() == null || booking.getTransactionId().isBlank()
                ? "-"
                : booking.getTransactionId();
        final double finalTotal = booking.getTotalPrice() == null ? 0.0 : booking.getTotalPrice();
        final String amountText = formatMoney(finalTotal, "INR");
        final String addressText = formatAddress(booking.getAddress());
        final String customerName = booking.getAddress() != null && booking.getAddress().getName() != null
                ? booking.getAddress().getName()
                : "-";
        final String customerMobile = booking.getAddress() != null && booking.getAddress().getUserMobileNumber() != null
                ? booking.getAddress().getUserMobileNumber()
                : "-";
        final String meetLink = resolveUserMeetingLink(booking, slot);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            addLogoWatermark(writer, document);

            FontFactory.registerDirectories();
            Font heroTitle = calibriFont(24, Font.BOLD, Color.WHITE);
            Font heroSub = calibriFont(11, Font.NORMAL, new Color(220, 231, 255));
            Font labelFont = calibriFont(10, Font.BOLD, new Color(64, 76, 98));
            Font valueFont = calibriFont(11, Font.NORMAL, new Color(22, 30, 48));
            Font sectionTitle = calibriFont(11, Font.BOLD, new Color(20, 36, 90));
            Font tableHead = calibriFont(10, Font.BOLD, Color.WHITE);
            Font tableBody = calibriFont(10.5f, Font.NORMAL, new Color(33, 41, 62));
            Font totalFont = calibriFont(12.5f, Font.BOLD, new Color(20, 36, 90));

            Color heroBg = new Color(24, 42, 96);
            Color panelBg = new Color(246, 249, 255);
            Color panelBorder = new Color(222, 231, 246);
            Color tableHeaderBg = new Color(45, 66, 137);

            PdfPTable hero = new PdfPTable(1);
            hero.setWidthPercentage(100);
            PdfPCell heroCell = new PdfPCell();
            heroCell.setBackgroundColor(heroBg);
            heroCell.setBorderColor(heroBg);
            heroCell.setPadding(16f);
            Paragraph heroTitleP = new Paragraph("Tax Invoice", heroTitle);
            heroTitleP.setSpacingAfter(5f);
            heroCell.addElement(heroTitleP);
            heroCell.addElement(new Paragraph("Puja Booking Receipt", calibriFont(14, Font.BOLD, Color.WHITE)));
            heroCell.addElement(new Paragraph("ORIGINAL FOR RECIPIENT", heroSub));
            hero.addCell(heroCell);
            hero.setSpacingAfter(10f);
            document.add(hero);

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(10f);
            meta.addCell(pdfPanelCell("Invoice No", orderId, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(pdfPanelCell("Invoice Date", LocalDate.now().toString(), labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(pdfPanelCell("Payment Mode", paymentMethod, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(pdfPanelCell("Status", formatStatus(booking.getStatus() == null ? null : booking.getStatus().name()), labelFont, valueFont, panelBg, panelBorder));
            document.add(meta);

            PdfPTable details = new PdfPTable(new float[]{1f, 1f});
            details.setWidthPercentage(100);
            details.setSpacingAfter(10f);
            details.addCell(pdfDetailBlock(
                    "Billed To",
                    new String[]{
                            "Name: " + customerName,
                            "User ID: " + userId,
                            "Mobile: " + customerMobile,
                            "Address: " + addressText
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            details.addCell(pdfDetailBlock(
                    "Booking Details",
                    new String[]{
                            "Puja: " + pujaName,
                            "Slot Time: " + slotTime,
                            "Transaction ID: " + txnId,
                            "Meeting: Google Meet"
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            document.add(details);

            PdfPTable table = new PdfPTable(new float[]{1.1f, 5.8f, 2.1f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(10f);
            table.addCell(pdfTableHeaderCell("Sr. No", tableHead, tableHeaderBg));
            table.addCell(pdfTableHeaderCell("Particulars", tableHead, tableHeaderBg));
            table.addCell(pdfTableHeaderCell("Amount", tableHead, tableHeaderBg));
            table.addCell(pdfTableTextCell("1", tableBody, Element.ALIGN_CENTER, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell(pujaName + " | Slot: " + slotTime, tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell(amountText, tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell("", tableBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            table.addCell(pdfTableTextCell("Taxable Amount", tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(pdfTableTextCell(amountText, tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(pdfTableTextCell("", tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell("IGST @ 0.00%", tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell(formatMoney(0.0, "INR"), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            table.addCell(pdfTableTextCell("", tableBody, Element.ALIGN_LEFT, new Color(232, 239, 255), panelBorder));
            table.addCell(pdfTableTextCell("Total Amount", totalFont, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));
            table.addCell(pdfTableTextCell(amountText, totalFont, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));
            document.add(table);

            PdfPTable summary = new PdfPTable(new float[]{2.7f, 1.3f});
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(10f);
            PdfPCell summaryLabel = new PdfPCell(new Phrase("Booked At", calibriFont(11, Font.BOLD, new Color(19, 40, 95))));
            summaryLabel.setPadding(10f);
            summaryLabel.setBorderColor(new Color(186, 205, 242));
            summaryLabel.setBackgroundColor(new Color(241, 246, 255));
            summary.addCell(summaryLabel);
            PdfPCell summaryValue = new PdfPCell(new Phrase(bookedAt, calibriFont(12, Font.BOLD, new Color(19, 40, 95))));
            summaryValue.setPadding(10f);
            summaryValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryValue.setBorderColor(new Color(186, 205, 242));
            summaryValue.setBackgroundColor(new Color(241, 246, 255));
            summary.addCell(summaryValue);
            document.add(summary);

            Paragraph meetPara = new Paragraph();
            meetPara.setSpacingAfter(4f);
            meetPara.add(new Chunk("Google Meet Link: ", calibriFont(10.5f, Font.BOLD, new Color(25, 47, 122))));
            Anchor meetAnchor = new Anchor(meetLink, calibriFont(10.5f, Font.UNDERLINE, new Color(25, 47, 122)));
            meetAnchor.setReference(meetLink);
            meetPara.add(meetAnchor);
            document.add(meetPara);

            Paragraph appPara = new Paragraph();
            appPara.setSpacingAfter(10f);
            appPara.add(new Chunk("App Link: ", calibriFont(10.5f, Font.BOLD, new Color(25, 47, 122))));
            Anchor appAnchor = new Anchor("https://astrologer.app", calibriFont(10.5f, Font.UNDERLINE, new Color(25, 47, 122)));
            appAnchor.setReference("https://astrologer.app");
            appPara.add(appAnchor);
            document.add(appPara);

            document.add(new Paragraph("This is a computer generated invoice and does not require signature.", calibriFont(9.5f, Font.NORMAL, new Color(84, 93, 112))));
            document.add(new Paragraph("For support: support@astrologer.app", calibriFont(9.5f, Font.NORMAL, new Color(84, 93, 112))));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate puja receipt PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell taxHeaderCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, calibriFont(11, Font.BOLD, Color.BLACK)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(238, 238, 238));
        cell.setPadding(8f);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(1.2f);
        return cell;
    }

    private PdfPCell taxBodyCell(String value, int align, Font font, float borderWidth) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(7f);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(borderWidth);
        return cell;
    }

    private Font calibriFont(float size, int style, Color color) {
        Font font = FontFactory.getFont("Calibri", size, style, color);
        if (font == null || font.getFamilyname() == null || "unknown".equalsIgnoreCase(font.getFamilyname())) {
            return new Font(Font.HELVETICA, size, style, color);
        }
        return font;
    }

    private PdfPCell pdfPanelCell(
            String label,
            String value,
            Font labelFont,
            Font valueFont,
            Color background,
            Color border
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        Paragraph labelP = new Paragraph(label == null || label.isBlank() ? "-" : label, labelFont);
        labelP.setSpacingAfter(3f);
        cell.addElement(labelP);
        cell.addElement(new Paragraph(value == null || value.isBlank() ? "-" : value, valueFont));
        return cell;
    }

    private PdfPCell pdfDetailBlock(
            String title,
            String[] lines,
            Font titleFont,
            Font lineFont,
            Color background,
            Color border
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        Paragraph titleP = new Paragraph(title == null || title.isBlank() ? "-" : title, titleFont);
        titleP.setSpacingAfter(6f);
        cell.addElement(titleP);
        for (String line : lines) {
            Paragraph row = new Paragraph(line == null || line.isBlank() ? "-" : line, lineFont);
            row.setSpacingAfter(2f);
            cell.addElement(row);
        }
        return cell;
    }

    private PdfPCell pdfTableHeaderCell(String value, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
        cell.setPadding(8f);
        cell.setBorderColor(background);
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment("Amount".equalsIgnoreCase(value) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell pdfTableTextCell(String value, Font font, int align, Color background, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(border);
        cell.setBackgroundColor(background);
        return cell;
    }

    private PdfPCell pdfInfoLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(225, 231, 240));
        cell.setBackgroundColor(new Color(245, 248, 255));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell pdfInfoValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "-" : text, font));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(225, 231, 240));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell pdfHeaderCell(String value) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(31, 47, 115));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(31, 47, 115));
        return cell;
    }

    private PdfPCell pdfBodyCell(String value, int align) {
        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(29, 35, 47));
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(8f);
        cell.setBorderColor(new Color(225, 231, 240));
        return cell;
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        if (address.getName() != null && !address.getName().isBlank()) parts.add(address.getName());
        if (address.getAddressLine1() != null && !address.getAddressLine1().isBlank()) parts.add(address.getAddressLine1());
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) parts.add(address.getAddressLine2());
        if (address.getLandmark() != null && !address.getLandmark().isBlank()) parts.add("Landmark: " + address.getLandmark());
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) parts.add(address.getDistrict());
        if (address.getCity() != null && !address.getCity().isBlank()) parts.add(address.getCity());
        if (address.getState() != null && !address.getState().isBlank()) parts.add(address.getState());
        if (address.getPincode() != null && !address.getPincode().isBlank()) parts.add("PIN " + address.getPincode());
        if (address.getUserMobileNumber() != null && !address.getUserMobileNumber().isBlank()) {
            parts.add("Mobile: " + address.getUserMobileNumber());
        }
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) return "-";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CONFIRMED" -> "Confirmed";
            case "PENDING" -> "Pending";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            default -> normalized;
        };
    }

    private void addLogoWatermark(PdfWriter writer, Document document) {
        try {
            byte[] logoBytes = loadLogoBytes();
            if (logoBytes == null || logoBytes.length == 0) {
                return;
            }
            Image watermark = Image.getInstance(logoBytes);
            watermark.scaleToFit(230, 230);

            Rectangle page = document.getPageSize();
            float x = (page.getWidth() - watermark.getScaledWidth()) / 2f;
            float y = (page.getHeight() - watermark.getScaledHeight()) / 2f;
            watermark.setAbsolutePosition(x, y);

            PdfContentByte under = writer.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.09f);
            under.saveState();
            under.setGState(state);
            under.addImage(watermark);
            under.restoreState();
        } catch (Exception ignored) {
            // Watermark is cosmetic; invoice generation should not fail because of this.
        }
    }

    private byte[] loadLogoBytes() {
        String[] candidates = new String[]{
                "branding/app-logo.png",
                "static/app-logo.png",
                "app-logo.png"
        };
        for (String path : candidates) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                return in.readAllBytes();
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String formatMoney(double amount, String currency) {
        final String c = currency == null ? "INR" : currency.toUpperCase();
        final String symbol = "INR".equals(c) ? "INR " : c + " ";
        return symbol + String.format(Locale.US, "%.2f", amount);
    }

    private String defaultGoogleMeetLink() {
        return "https://meet.google.com/new";
    }

    private String resolveActualMeetingLink(PujaBooking booking) {
        if (booking == null || booking.getMeetingLink() == null || booking.getMeetingLink().isBlank()) {
            return defaultGoogleMeetLink();
        }
        return booking.getMeetingLink().trim();
    }

    private String normalizePublicBaseUrl() {
        String raw = appPublicBaseUrl == null ? "" : appPublicBaseUrl.trim();
        if (raw.isEmpty()) {
            raw = "http://localhost:1234";
        }
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private String formatJoinSlotParam(LocalDateTime slotTime) {
        if (slotTime == null) return "na";
        return slotTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private String formatJoinDateParam(LocalDateTime slotTime) {
        if (slotTime == null) return "na";
        return slotTime.toLocalDate().toString();
    }

    private String ensureJoinToken(PujaBooking booking) {
        if (booking == null) {
            return "";
        }
        String existing = booking.getJoinToken() == null ? "" : booking.getJoinToken().trim();
        if (!existing.isEmpty()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString().replace("-", "");
        booking.setJoinToken(generated);
        bookingRepo.save(booking);
        return generated;
    }

    public String resolveUserMeetingLink(PujaBooking booking, PujaSlot slot) {
        if (booking == null || booking.getId() == null || booking.getId() <= 0) {
            return defaultGoogleMeetLink();
        }
        String token = ensureJoinToken(booking);
        String orderId = PujaOrderIdHelper.build(booking.getUserId(), booking.getId());
        String slotParam = formatJoinSlotParam(slot == null ? null : slot.getSlotTime());
        String dateParam = formatJoinDateParam(slot == null ? null : slot.getSlotTime());
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String encodedSlot = URLEncoder.encode(slotParam, StandardCharsets.UTF_8);
        String encodedDate = URLEncoder.encode(dateParam, StandardCharsets.UTF_8);
        return normalizePublicBaseUrl() + "/puja/join/" + orderId + "?token=" + encodedToken + "&slot=" + encodedSlot + "&date=" + encodedDate;
    }

    public Map<String, Object> getJoinAccess(String orderId, String token) {
        return getJoinAccess(orderId, token, null);
    }

    public Map<String, Object> getJoinAccess(String orderId, String token, String expectedDate) {
        if (orderId == null || orderId.isBlank()) {
            throw new RuntimeException("Valid orderId is required");
        }
        Long bookingId = parseBookingId(orderId);
        PujaBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Puja booking not found: " + orderId));
        PujaSlot slot = booking.getSlotId() == null
                ? null
                : slotRepo.findById(booking.getSlotId()).orElse(null);

        String storedToken = ensureJoinToken(booking);
        String providedToken = token == null ? "" : token.trim();
        if (providedToken.isEmpty() || !storedToken.equals(providedToken)) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("status", false);
            blocked.put("allowed", false);
            blocked.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
            blocked.put("message", "Invalid or expired join link.");
            return blocked;
        }

        if (slot == null || slot.getSlotTime() == null) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("status", false);
            blocked.put("allowed", false);
            blocked.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
            blocked.put("message", "Puja slot is not assigned yet. Please wait for admin confirmation.");
            return blocked;
        }

        LocalDateTime slotTime = slot.getSlotTime();
        String requestedDate = expectedDate == null ? "" : expectedDate.trim();
        if (!requestedDate.isEmpty() && !"na".equalsIgnoreCase(requestedDate)
                && !requestedDate.equals(slotTime.toLocalDate().toString())) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("status", false);
            blocked.put("allowed", false);
            blocked.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
            blocked.put("message", "Join link is not valid for this puja date.");
            return blocked;
        }

        LocalDateTime joinOpensAt = slotTime.minusMinutes(JOIN_ALLOWED_MINUTES_BEFORE_SLOT);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(joinOpensAt)) {
            long mins = Duration.between(now, joinOpensAt).toMinutes();
            if (Duration.between(now, joinOpensAt).getSeconds() % 60 != 0) {
                mins = mins + 1;
            }
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("status", false);
            blocked.put("allowed", false);
            blocked.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
            blocked.put("slotTime", slotTime);
            blocked.put("joinOpensAt", joinOpensAt);
            blocked.put("minutesToOpen", Math.max(mins, 1));
            blocked.put("message", "Join will be enabled 10 minutes before your puja slot.");
            return blocked;
        }

        Map<String, Object> allowed = new LinkedHashMap<>();
        allowed.put("status", true);
        allowed.put("allowed", true);
        allowed.put("orderId", PujaOrderIdHelper.build(booking.getUserId(), bookingId));
        allowed.put("slotTime", slotTime);
        allowed.put("joinOpensAt", joinOpensAt);
        allowed.put("meetingLink", resolveActualMeetingLink(booking));
        allowed.put("message", "Join access granted.");
        return allowed;
    }

    private boolean sendPujaReceiptIfEmailAvailable(PujaBooking booking, Puja puja, PujaSlot slot) {
        try {
            if (!isPaymentConfirmed(booking)) return false;

            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
            String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) return false;

            Long bookingId = booking.getId() == null ? 0L : booking.getId();
            String subject = "Your Astrologer Puja Receipt - " + PujaOrderIdHelper.build(booking.getUserId(), bookingId);
            String html = buildPujaReceiptHtml(booking, puja, slot);
            byte[] pdf = buildPujaReceiptPdf(booking, puja, slot);
            emailService.sendEmailWithAttachmentAsync(
                    toEmail,
                    subject,
                    html,
                    "invoice.pdf",
                    pdf,
                    "application/pdf"
            );
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean sendCalendarInviteIfEmailAvailable(PujaBooking booking) {
        return sendCalendarInviteIfEmailAvailable(booking, true);
    }

    private boolean sendCalendarInviteIfEmailAvailable(PujaBooking booking, boolean requireSpiritualReady) {
        try {
            if (!isPaymentConfirmed(booking)) return false;

            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
            String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) return false;

            Long bookingId = booking.getId() == null ? 0L : booking.getId();
            PujaBookingSpiritualDetail spiritual = pujaBookingSpiritualDetailRepository
                    .findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                    .orElse(null);
            if (requireSpiritualReady
                    && (spiritual == null
                    || spiritual.getRashiMasterId() == null
                    || spiritual.getNakshatraMasterId() == null)) {
                return false;
            }

            Puja puja = booking.getPujaId() == null ? null : pujaRepo.findById(booking.getPujaId()).orElse(null);
            PujaSlot slot = booking.getSlotId() == null ? null : slotRepo.findById(booking.getSlotId()).orElse(null);
            if (slot == null || slot.getSlotTime() == null) return false;

            String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                    ? "Puja Booking"
                    : puja.getName();
            String devoteeName = profile == null || profile.getName() == null || profile.getName().isBlank()
                    ? "Devotee"
                    : profile.getName().trim();
            String subject = "Puja Invitation - " + PujaOrderIdHelper.build(booking.getUserId(), bookingId);
            String meetLink = resolveUserMeetingLink(booking, slot);
            String slotTimeText = formatDateTime(slot.getSlotTime());
            String html = buildPujaConfirmationMailHtml(
                    booking,
                    devoteeName,
                    spiritual == null ? "-" : spiritual.getGotraName(),
                    spiritual == null ? "-" : spiritual.getRashiName(),
                    spiritual == null ? "-" : spiritual.getNakshatraName(),
                    pujaName,
                    slotTimeText,
                    meetLink
            );

            byte[] ics = buildCalendarInviteIcs(booking, pujaName, slot, meetLink);
            emailService.sendEmailWithAttachmentAsync(
                    toEmail,
                    subject,
                    html,
                    "invite.ics",
                    ics,
                    "text/calendar; charset=UTF-8"
            );
            return true;
        } catch (Exception ignored) {
            // Booking flow should not fail if invite mail cannot be generated/sent.
            return false;
        }
    }

    private int sendSamagriMailToBookedUsers(Long pujaId) {
        if (pujaId == null || pujaId <= 0) return 0;
        try {
            Puja puja = pujaRepo.findById(pujaId).orElse(null);
            List<PujaBooking> bookings = bookingRepo.findByPujaIdOrderByBookedAtDesc(pujaId);
            int queued = 0;
            for (PujaBooking booking : bookings) {
                if (!isPaymentConfirmed(booking) || !isSamagriRequiredForBooking(booking)) continue;

                MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
                String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
                if (toEmail.isEmpty()) continue;

                PujaSlot slot = booking.getSlotId() == null ? null : slotRepo.findById(booking.getSlotId()).orElse(null);
                String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                        ? "Puja Booking"
                        : puja.getName();
                String meetLink = resolveUserMeetingLink(booking, slot);
                String slotTimeText = slot == null || slot.getSlotTime() == null ? "-" : formatDateTime(slot.getSlotTime());
                String samagriSubject = "Puja Samagri List - " + PujaOrderIdHelper.build(booking.getUserId(), booking.getId());
                String samagriHtml = buildPujaSamagriMailHtml(booking, pujaName, slotTimeText, meetLink);
                emailService.sendEmailAsync(toEmail, samagriSubject, samagriHtml);
                queued++;
            }
            return queued;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean sendSamagriMailForBooking(PujaBooking booking) {
        if (booking == null || booking.getPujaId() == null || booking.getPujaId() <= 0) return false;
        if (!isSamagriRequiredForBooking(booking)) return false;
        if (!isPaymentConfirmed(booking)) return false;
        try {
            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(booking.getUserId()).orElse(null);
            String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) return false;

            Puja puja = pujaRepo.findById(booking.getPujaId()).orElse(null);
            PujaSlot slot = booking.getSlotId() == null ? null : slotRepo.findById(booking.getSlotId()).orElse(null);
            String pujaName = puja == null || puja.getName() == null || puja.getName().isBlank()
                    ? "Puja Booking"
                    : puja.getName();
            String meetLink = resolveUserMeetingLink(booking, slot);
            String slotTimeText = slot == null || slot.getSlotTime() == null ? "-" : formatDateTime(slot.getSlotTime());
            String samagriSubject = "Puja Samagri List - " + PujaOrderIdHelper.build(booking.getUserId(), booking.getId());
            String samagriHtml = buildPujaSamagriMailHtml(booking, pujaName, slotTimeText, meetLink);
            emailService.sendEmailAsync(toEmail, samagriSubject, samagriHtml);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isPaymentConfirmed(PujaBooking booking) {
        if (booking == null) return false;
        if (booking.getStatus() != PujaBooking.BookingStatus.CONFIRMED) return false;
        final String tx = booking.getTransactionId() == null ? "" : booking.getTransactionId().trim();
        return !tx.isEmpty();
    }

    private String buildPujaConfirmationMailHtml(
            PujaBooking booking,
            String devoteeName,
            String gotraName,
            String rashiName,
            String nakshatraName,
            String pujaName,
            String slotTimeText,
            String meetLink
    ) {
        String displayName = devoteeName == null || devoteeName.isBlank() ? "Devotee" : devoteeName.trim();
        String displayGotra = gotraName == null || gotraName.isBlank() ? "-" : gotraName.trim();
        String displayRashi = rashiName == null || rashiName.isBlank() ? "-" : rashiName.trim();
        String displayNakshatra = nakshatraName == null || nakshatraName.isBlank() ? "-" : nakshatraName.trim();
        String detailsHtml = """
                <div style="text-align:center;margin-bottom:12px;">
                  <div style="font-size:34px;line-height:1;color:#9a6b2f;">ॐ</div>
                  <div style="font-size:40px;font-weight:700;letter-spacing:.5px;color:#5d381f;">ASTRO ADHYAAY</div>
                  <div style="font-size:30px;margin-top:8px;color:#5d381f;">You are cordially invited...</div>
                  <div style="font-size:46px;margin-top:8px;font-weight:700;color:#4b2814;">Mr./Ms. %s</div>
                  <div style="font-size:23px;margin-top:6px;color:#603d21;">(Gotra: %s | Rashi: %s | Nakshatra: %s)</div>
                  <div style="font-size:27px;margin-top:8px;color:#5c3820;">Your gracious presence is deeply valued...</div>
                </div>
                <div style="margin-top:16px;font-size:31px;font-weight:700;color:#4f2c18;">🪔 Ceremony Details:</div>
                <ul style="margin:8px 0 0 24px;padding:0;font-size:29px;line-height:1.5;color:#4f2c18;">
                  <li><strong>Puja:</strong> %s</li>
                  <li><strong>Date & Time:</strong> %s</li>
                </ul>
                <div style="margin-top:14px;font-size:31px;font-weight:700;color:#4f2c18;">🔗 Join the Ceremony:</div>
                <div style="margin-top:6px;font-size:28px;word-break:break-all;">
                  <a href="%s" style="color:#1f5f9e;text-decoration:underline;">%s</a>
                </div>
                <div style="margin-top:20px;text-align:right;font-size:24px;line-height:1.45;color:#5c3820;">
                  With Sincere Regards & Blessings,<br/>
                  The Astro Adhyaay Pooja Team
                </div>
                """.formatted(
                escapeHtml(displayName),
                escapeHtml(displayGotra),
                escapeHtml(displayRashi),
                escapeHtml(displayNakshatra),
                escapeHtml(pujaName),
                escapeHtml(slotTimeText),
                escapeHtml(meetLink),
                escapeHtml(meetLink)
        );
        return wrapPujaMailTemplate(
                detailsHtml,
                "Calendar invite is attached in this email."
        );
    }

    private String buildPujaSamagriMailHtml(
            PujaBooking booking,
            String pujaName,
            String slotTimeText,
            String meetLink
    ) {
        List<Map<String, Object>> samagri = getPujaSamagriForMobile(booking.getPujaId() == null ? 0L : booking.getPujaId());
        StringBuilder listHtml = new StringBuilder();
        if (samagri.isEmpty()) {
            listHtml.append("<li>Please keep basic puja essentials ready.</li>");
        } else {
            for (Map<String, Object> item : samagri) {
                String name = item.get("samagriName") == null ? "-" : item.get("samagriName").toString();
                String qty = item.get("quantity") == null ? "" : item.get("quantity").toString().trim();
                String suffix = qty.isEmpty() ? "" : (" - " + qty);
                listHtml.append("<li>").append(escapeHtml(name + suffix)).append("</li>");
            }
        }
        String detailsHtml = """
                <div style="text-align:center;">
                  <div style="font-size:34px;line-height:1;color:#9a6b2f;">ॐ</div>
                  <div style="font-size:40px;font-weight:700;letter-spacing:.5px;color:#5d381f;">ASTRO ADHYAAY</div>
                  <div style="font-size:54px;margin-top:10px;font-weight:700;color:#4b2814;">Authentic Puja Samagri List</div>
                </div>
                <ul style="margin:14px 0 0 24px;padding:0;font-size:29px;line-height:1.5;color:#4f2c18;">
                  %s
                </ul>
                <div style="margin-top:14px;border-top:1px solid #bfa172;padding-top:12px;font-size:25px;line-height:1.5;color:#4f2c18;">
                  <strong>Puja:</strong> %s<br/>
                  <strong>Slot:</strong> %s<br/>
                  <strong>Join:</strong> <a href="%s" style="color:#1f5f9e;text-decoration:underline;">Google Meet Link</a>
                </div>
                <div style="margin-top:16px;text-align:center;font-size:27px;font-weight:700;color:#4f2c18;">
                  WEAR INDIAN TRADITIONAL WEAR
                </div>
                <div style="text-align:center;margin-top:10px;font-size:22px;color:#5b3920;">
                  👩 Saree / Suit &nbsp; &nbsp; | &nbsp; &nbsp; 🙏 Dhoti Kurta / Kurta Pajama
                </div>
                """.formatted(
                listHtml.toString(),
                escapeHtml(pujaName),
                escapeHtml(slotTimeText),
                escapeHtml(meetLink)
        );
        return wrapPujaMailTemplate(detailsHtml, null);
    }

    private String wrapPujaMailTemplate(String contentHtml, String footerNote) {
        String noteHtml = (footerNote == null || footerNote.isBlank())
                ? ""
                : """
                  <p style="text-align:center;color:#6b5234;font-size:13px;margin:10px 0 0 0;">
                    %s
                  </p>
                """.formatted(escapeHtml(footerNote));

        return """
                <html>
                <body style="margin:0;padding:0;background:#eadfc7;font-family:Georgia,'Times New Roman',serif;color:#4a2f1a;">
                  <div style="max-width:900px;margin:0 auto;padding:16px 10px;">
                    <table role="presentation" style="width:100%%;border-collapse:separate;border-spacing:0;background:#f4e8cf;border:1px solid #c8ab73;border-radius:10px;overflow:hidden;box-shadow:0 4px 16px rgba(92,67,38,.15);">
                      <tr>
                        <td style="width:30px;background:#d0ab75;">
                          <div style="height:100%%;min-height:280px;background:linear-gradient(180deg,#d6b47d 0%%,#be8f57 50%%,#d6b47d 100%%);"></div>
                        </td>
                        <td style="padding:24px 26px;background:radial-gradient(circle at top,#f9f0db 0%%,#f1e2c2 66%%,#ead7b4 100%%);">
                          %s
                        </td>
                        <td style="width:30px;background:#d0ab75;">
                          <div style="height:100%%;min-height:280px;background:linear-gradient(180deg,#d6b47d 0%%,#be8f57 50%%,#d6b47d 100%%);"></div>
                        </td>
                      </tr>
                    </table>
                    %s
                  </div>
                </body>
                </html>
                """.formatted(contentHtml, noteHtml);
    }

    private byte[] buildCalendarInviteIcs(PujaBooking booking, String pujaName, PujaSlot slot, String meetLink) {
        LocalDateTime start = slot == null || slot.getSlotTime() == null ? LocalDateTime.now().plusHours(1) : slot.getSlotTime();
        LocalDateTime end = start.plusMinutes(60);
        if (slot != null && slot.getSlotTime() != null && booking.getPujaId() != null) {
            Puja p = pujaRepo.findById(booking.getPujaId()).orElse(null);
            if (p != null && p.getDurationMinutes() > 0) {
                end = start.plusMinutes(p.getDurationMinutes());
            }
        }

        DateTimeFormatter utcFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dtStart = start.atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).format(utcFmt);
        String dtEnd = end.atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).format(utcFmt);
        String dtStamp = LocalDateTime.now().atZone(ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).format(utcFmt);
        String uid = "PUJA-" + (booking.getId() == null ? UUID.randomUUID() : booking.getId()) + "@astrologer.app";

        String ics = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Astrologer//Puja Booking//EN
                CALSCALE:GREGORIAN
                METHOD:PUBLISH
                BEGIN:VEVENT
                UID:%s
                DTSTAMP:%s
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                DESCRIPTION:Puja booking reminder. Join using Google Meet link: %s
                LOCATION:%s
                STATUS:CONFIRMED
                BEGIN:VALARM
                TRIGGER:-PT10M
                ACTION:DISPLAY
                DESCRIPTION:Puja starts in 10 minutes
                END:VALARM
                END:VEVENT
                END:VCALENDAR
                """.formatted(
                uid,
                dtStamp,
                dtStart,
                dtEnd,
                sanitizeIcsText(pujaName),
                sanitizeIcsText(meetLink),
                sanitizeIcsText(meetLink)
        );
        return ics.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String sanitizeIcsText(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value); // HH:mm or HH:mm:ss
        } catch (DateTimeParseException ignored) {
            if (value != null && value.length() == 5) {
                return LocalTime.parse(value + ":00");
            }
            throw ignored;
        }
    }

    public PujaBooking reschedulePuja(Long userId, Long bookingId, Long newSlotId) {
        PujaBooking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new RuntimeException("Booking not found for user"));

        if (booking.getStatus() != PujaBooking.BookingStatus.CONFIRMED
                && booking.getStatus() != PujaBooking.BookingStatus.PENDING) {
            throw new RuntimeException("Only confirmed/pending booking can be rescheduled");
        }

        PujaSlot currentSlot = slotRepo.findById(booking.getSlotId())
                .orElseThrow(() -> new RuntimeException("Current slot not found"));
        LocalDateTime cutoff = currentSlot.getSlotTime().minusDays(1);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new RuntimeException("Rescheduling not allowed within 1 day of puja slot");
        }

        PujaSlot newSlot = slotRepo.findById(newSlotId)
                .orElseThrow(() -> new RuntimeException("New slot not found"));

        if (!newSlot.getPujaId().equals(booking.getPujaId())) {
            throw new RuntimeException("Selected slot does not belong to this puja");
        }
        if (newSlot.getStatus() != PujaSlot.SlotStatus.AVAILABLE) {
            throw new RuntimeException("Selected slot is already filled");
        }
        if (newSlot.getSlotTime() == null || !newSlot.getSlotTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Selected slot must be in future");
        }

        // Free current slot and reserve new slot
        currentSlot.setStatus(PujaSlot.SlotStatus.AVAILABLE);
        slotRepo.save(currentSlot);

        newSlot.setStatus(PujaSlot.SlotStatus.BOOKED);
        slotRepo.save(newSlot);

        booking.setSlotId(newSlot.getId());
        booking.setStatus(PujaBooking.BookingStatus.CONFIRMED);
        PujaBooking saved = bookingRepo.save(booking);
        try {
            Puja puja = saved.getPujaId() == null ? null : pujaRepo.findById(saved.getPujaId()).orElse(null);
            pujaBookingNotificationService.notifyAdminAndPandit(saved, puja, newSlot);
        } catch (Exception ignored) {
        }
        return saved;
    }
}
