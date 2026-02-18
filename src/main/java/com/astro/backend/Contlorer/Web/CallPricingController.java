package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.CallPricingUpdateRequest;
import com.astro.backend.Services.CallPricingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web/call-pricing")
@RequiredArgsConstructor
public class CallPricingController {

    private final CallPricingConfigService callPricingConfigService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPricing() {
        return ResponseEntity.ok(buildResponse(callPricingConfigService.getOrCreateConfig(), "Call pricing fetched successfully"));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePricing(@Valid @RequestBody CallPricingUpdateRequest request) {
        try {
            CallPricingConfigService.CallPricingConfig saved = callPricingConfigService.saveConfig(
                    request.getAudioRatePerMin(),
                    request.getVideoRatePerMin(),
                    request.getAudioFreeMinutes(),
                    request.getVideoFreeMinutes()
            );
            return ResponseEntity.ok(buildResponse(saved, "Call pricing updated successfully"));
        } catch (RuntimeException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "Failed to update call pricing: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    private Map<String, Object> buildResponse(CallPricingConfigService.CallPricingConfig cfg, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("billingUnitMinutes", CallPricingConfigService.BILLING_UNIT_MINUTES);
        body.put("audioRatePer30Min", cfg.getAudioRatePerMin());
        body.put("videoRatePer30Min", cfg.getVideoRatePerMin());
        body.put("audioRatePerMin", cfg.getAudioRatePerMin());
        body.put("videoRatePerMin", cfg.getVideoRatePerMin());
        body.put("audioFreeMinutes", cfg.getAudioFreeMinutes());
        body.put("videoFreeMinutes", cfg.getVideoFreeMinutes());
        return body;
    }
}
