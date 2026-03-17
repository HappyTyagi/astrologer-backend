package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.PujaSamagriCartSyncRequest;
import com.astro.backend.Services.PujaSamagriCartService;
import com.astro.backend.ResponseDTO.PujaSamagriCartResponse;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/puja-samagri/cart")
@RequiredArgsConstructor
public class PujaSamagriCartController {

    private final PujaSamagriCartService cartService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<PujaSamagriCartResponse>> syncCart(
            @RequestBody PujaSamagriCartSyncRequest request
    ) {
        try {
            final PujaSamagriCartResponse response = cartService.syncCart(request);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            response,
                            true,
                            "Cart synced successfully"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "CART_SYNC_FAILED"
                    )
            );
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PujaSamagriCartResponse>> getCart(@PathVariable Long userId) {
        try {
            final PujaSamagriCartResponse response = cartService.getCart(userId);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            response,
                            true,
                            "Cart fetched successfully"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "CART_FETCH_FAILED"
                    )
            );
        }
    }

    @DeleteMapping("/{userId}/item/{samagriMasterId}")
    public ResponseEntity<ApiResponse<PujaSamagriCartResponse>> removeItem(
            @PathVariable Long userId,
            @PathVariable Long samagriMasterId
    ) {
        try {
            final PujaSamagriCartResponse response = cartService.removeItem(userId, samagriMasterId);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            response,
                            true,
                            "Item removed from cart"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "CART_REMOVE_FAILED"
                    )
            );
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<ApiResponse<PujaSamagriCartResponse>> clearCart(@PathVariable Long userId) {
        try {
            final PujaSamagriCartResponse response = cartService.clearCart(userId);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            response,
                            true,
                            "Cart cleared successfully"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "CART_CLEAR_FAILED"
                    )
            );
        }
    }
}
