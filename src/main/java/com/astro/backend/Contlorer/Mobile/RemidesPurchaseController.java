package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.RemidesPurchaseRequest;
import com.astro.backend.Services.RemidesPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/remides/purchase")
@RequiredArgsConstructor
public class RemidesPurchaseController {

    private final RemidesPurchaseService remidesPurchaseService;

    @PostMapping
    public ResponseEntity<?> purchaseProducts(@RequestBody RemidesPurchaseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(remidesPurchaseService.purchaseCart(request));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> purchaseHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(remidesPurchaseService.getPurchaseHistory(userId));
    }

    @GetMapping("/history/{userId}/realtime")
    public ResponseEntity<?> purchaseHistoryRealtime(
            @PathVariable Long userId,
            @RequestParam(value = "since", required = false) String since
    ) {
        LocalDateTime sinceTime = null;
        if (since != null && !since.isBlank()) {
            sinceTime = LocalDateTime.parse(since);
        }
        return ResponseEntity.ok(remidesPurchaseService.getPurchaseHistoryRealtime(userId, sinceTime));
    }
}
