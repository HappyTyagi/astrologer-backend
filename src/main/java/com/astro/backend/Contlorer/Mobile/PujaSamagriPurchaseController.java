package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.PujaSamagriPurchaseRequest;
import com.astro.backend.Services.PujaSamagriPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/puja-samagri/purchase")
@RequiredArgsConstructor
public class PujaSamagriPurchaseController {

    private final PujaSamagriPurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<?> purchaseProducts(@RequestBody PujaSamagriPurchaseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(purchaseService.purchaseCart(request));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
