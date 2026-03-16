package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.PujaSamagriCartSyncRequest;
import com.astro.backend.Services.PujaSamagriCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/puja-samagri/cart")
@RequiredArgsConstructor
public class PujaSamagriCartController {

    private final PujaSamagriCartService cartService;

    @PostMapping("/sync")
    public ResponseEntity<?> syncCart(@RequestBody PujaSamagriCartSyncRequest request) {
        try {
            return ResponseEntity.ok(cartService.syncCart(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(cartService.getCart(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @DeleteMapping("/{userId}/item/{samagriMasterId}")
    public ResponseEntity<?> removeItem(@PathVariable Long userId, @PathVariable Long samagriMasterId) {
        try {
            return ResponseEntity.ok(cartService.removeItem(userId, samagriMasterId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(cartService.clearCart(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    private Map<String, Object> error(Exception e) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", false);
        error.put("message", e.getMessage());
        return error;
    }
}
