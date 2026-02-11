package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.RemidesCartSyncRequest;
import com.astro.backend.Services.RemidesCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/remides/cart")
@RequiredArgsConstructor
public class RemidesCartController {

    private final RemidesCartService remidesCartService;

    @PostMapping("/sync")
    public ResponseEntity<?> syncCart(@RequestBody RemidesCartSyncRequest request) {
        try {
            return ResponseEntity.ok(remidesCartService.syncCart(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(remidesCartService.getCart(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @DeleteMapping("/{userId}/item/{remidesId}")
    public ResponseEntity<?> removeItem(@PathVariable Long userId, @PathVariable Long remidesId) {
        try {
            return ResponseEntity.ok(remidesCartService.removeItem(userId, remidesId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e));
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(remidesCartService.clearCart(userId));
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
