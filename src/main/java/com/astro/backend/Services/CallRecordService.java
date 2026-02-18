package com.astro.backend.Services;

import com.astro.backend.Entity.CallRecord;
import com.astro.backend.Entity.User;
import com.astro.backend.EnumFile.Role;
import com.astro.backend.Repositry.CallRecordRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.CallRecordRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallRecordService {

    private final CallRecordRepository callRecordRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final CallPricingConfigService callPricingConfigService;

    @Getter
    @Builder
    public static class SaveCallResult {
        private CallRecord record;
        private boolean chargeSkipped;
    }

    public SaveCallResult saveRecord(CallRecordRequest request) {
        String normalizedCallId = request.getCallId().trim();
        Optional<CallRecord> existing = callRecordRepository.findTopByCallIdOrderByCreatedAtDesc(normalizedCallId);
        if (existing.isPresent()) {
            CallRecord record = existing.get();
            int existingDuration = record.getDurationSeconds() == null ? 0 : record.getDurationSeconds();
            int incomingDuration = request.getDurationSeconds() == null ? 0 : Math.max(0, request.getDurationSeconds());
            // Idempotency guard: avoid duplicate wallet debit and duplicate record inserts.
            if (incomingDuration > existingDuration
                    || (request.getEndedAtIso() != null && !request.getEndedAtIso().isBlank())) {
                record.setDurationSeconds(Math.max(existingDuration, incomingDuration));
                if (request.getEndedAtIso() != null && !request.getEndedAtIso().isBlank()) {
                    record.setEndedAt(parseIso(request.getEndedAtIso()));
                }
                if (request.getEndReason() != null && !request.getEndReason().isBlank()) {
                    record.setEndReason(request.getEndReason().trim().toLowerCase());
                }
                callRecordRepository.save(record);
            }
            return SaveCallResult.builder()
                    .record(record)
                    .chargeSkipped(true)
                    .build();
        }

        String normalizedType = callPricingConfigService.normalizeCallType(request.getCallType());
        CallPricingConfigService.CallPricingConfig pricing = callPricingConfigService.getOrCreateConfig();

        int durationSeconds = request.getDurationSeconds() == null ? 0 : Math.max(0, request.getDurationSeconds());
        int totalMinutes = (int) Math.ceil(durationSeconds / 60.0d);
        int freeMinutes = "video".equals(normalizedType) ? pricing.getVideoFreeMinutes() : pricing.getAudioFreeMinutes();
        int billableMinutes = Math.max(0, totalMinutes - Math.max(0, freeMinutes));
        int billingUnitMinutes = CallPricingConfigService.BILLING_UNIT_MINUTES;
        int billedUnits = (int) Math.ceil(billableMinutes / (double) billingUnitMinutes);
        double ratePerUnit = "video".equals(normalizedType) ? pricing.getVideoRatePerMin() : pricing.getAudioRatePerMin();
        double chargedAmount = billedUnits * Math.max(0.0, ratePerUnit);

        Long chargedUserIdLong = resolveChargeableUserId(request.getCallerUserId(), request.getReceiverUserId());
        Integer chargedUserId = chargedUserIdLong == null ? null : chargedUserIdLong.intValue();
        boolean walletDebited = false;
        boolean chargeSkipped = chargedUserIdLong == null || chargedAmount <= 0.0;

        if (!chargeSkipped) {
            String ref = "CALL-" + normalizedCallId;
            String desc = ("video".equals(normalizedType) ? "Video" : "Audio")
                    + " call charge (" + billedUnits + " x " + billingUnitMinutes + " min slab)";
            walletDebited = walletService.debit(chargedUserIdLong, chargedAmount, ref, desc);
        }

        CallRecord record = CallRecord.builder()
                .chatId(request.getChatId().trim())
                .callId(normalizedCallId)
                .callerUserId(request.getCallerUserId())
                .receiverUserId(request.getReceiverUserId())
                .callType(normalizedType)
                .endReason(request.getEndReason().trim().toLowerCase())
                .durationSeconds(durationSeconds)
                .totalMinutes(totalMinutes)
                .freeMinutesApplied(Math.max(0, freeMinutes))
                .billableMinutes(billableMinutes)
                .billingUnitMinutes(billingUnitMinutes)
                .billedUnits(billedUnits)
                .ratePerUnit(ratePerUnit)
                .ratePerMinute(ratePerUnit)
                .chargedAmount(chargedAmount)
                .chargedUserId(chargedUserId)
                .walletDebited(walletDebited)
                .startedAt(parseIso(request.getStartedAtIso()))
                .endedAt(parseIso(request.getEndedAtIso()))
                .build();
        CallRecord saved = callRecordRepository.save(record);
        return SaveCallResult.builder()
                .record(saved)
                .chargeSkipped(chargeSkipped)
                .build();
    }

    private LocalDateTime parseIso(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private Long resolveChargeableUserId(Integer callerUserId, Integer receiverUserId) {
        Optional<Long> caller = toChargeableUserId(callerUserId);
        if (caller.isPresent()) return caller.get();
        return toChargeableUserId(receiverUserId).orElse(null);
    }

    private Optional<Long> toChargeableUserId(Integer userId) {
        if (userId == null || userId <= 0) return Optional.empty();
        return userRepository.findById(Long.valueOf(userId))
                .filter(user -> user.getRole() != Role.ADMIN)
                .filter(user -> user.getRole() != Role.ASTROLOGER)
                .map(User::getId);
    }
}
