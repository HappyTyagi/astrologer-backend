package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.CallRecord;
import com.astro.backend.RequestDTO.CallRecordRequest;
import com.astro.backend.ResponseDTO.CallRecordResponse;
import com.astro.backend.Services.CallPricingConfigService;
import com.astro.backend.Services.CallRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/call")
@RequiredArgsConstructor
public class CallRecordController {

    private final CallRecordService callRecordService;
    private final CallPricingConfigService callPricingConfigService;

    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing() {
        CallPricingConfigService.CallPricingConfig cfg = callPricingConfigService.getOrCreateConfig();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("billingUnitMinutes", CallPricingConfigService.BILLING_UNIT_MINUTES);
        body.put("audioRatePer30Min", cfg.getAudioRatePerMin());
        body.put("videoRatePer30Min", cfg.getVideoRatePerMin());
        body.put("audioRatePerMin", cfg.getAudioRatePerMin());
        body.put("videoRatePerMin", cfg.getVideoRatePerMin());
        body.put("audioFreeMinutes", cfg.getAudioFreeMinutes());
        body.put("videoFreeMinutes", cfg.getVideoFreeMinutes());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/record")
    public ResponseEntity<CallRecordResponse> saveCallRecord(@Valid @RequestBody CallRecordRequest request) {
        try {
            CallRecordService.SaveCallResult result = callRecordService.saveRecord(request);
            CallRecord saved = result.getRecord();
            boolean debited = Boolean.TRUE.equals(saved.getWalletDebited());
            boolean chargeSkipped = result.isChargeSkipped();

            return ResponseEntity.ok(
                    CallRecordResponse.builder()
                            .success(true)
                            .message(chargeSkipped || debited
                                    ? "Call record saved"
                                    : "Call record saved, but wallet debit failed due to insufficient balance")
                            .id(saved.getId())
                            .callType(saved.getCallType())
                            .totalMinutes(saved.getTotalMinutes())
                            .freeMinutesApplied(saved.getFreeMinutesApplied())
                            .billableMinutes(saved.getBillableMinutes())
                            .billingUnitMinutes(saved.getBillingUnitMinutes())
                            .billedUnits(saved.getBilledUnits())
                            .ratePerUnit(saved.getRatePerUnit())
                            .ratePerMinute(saved.getRatePerMinute())
                            .chargedAmount(saved.getChargedAmount())
                            .chargedUserId(saved.getChargedUserId())
                            .walletDebited(saved.getWalletDebited())
                            .chargeSkipped(chargeSkipped)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CallRecordResponse.builder()
                            .success(false)
                            .message("Failed to save call record: " + e.getMessage())
                            .build());
        }
    }
}
