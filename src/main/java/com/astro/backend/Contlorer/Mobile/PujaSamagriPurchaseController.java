package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.PujaSamagriPurchaseRequest;
import com.astro.backend.Services.PujaSamagriPurchaseService;
import com.astro.backend.ResponseDTO.PujaSamagriPurchaseResponse;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/puja-samagri/purchase")
@RequiredArgsConstructor
public class PujaSamagriPurchaseController {

    private final PujaSamagriPurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<ApiResponse<PujaSamagriPurchaseResponse>> purchaseProducts(
            @RequestBody PujaSamagriPurchaseRequest request
    ) {
        try {
            final PujaSamagriPurchaseResponse response = purchaseService.purchaseCart(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                            ResponseUtils.createSuccessResponse(
                                    response,
                                    true,
                                    "Products purchased successfully"
                            )
                    );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "PURCHASE_FAILED"
                    )
            );
        }
    }
}
