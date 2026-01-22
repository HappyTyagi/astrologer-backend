package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.AstrologerDistrictPriceRequest;
import com.astro.backend.ResponseDTO.AstrologerDistrictPriceResponse;
import com.astro.backend.Services.AstrologerDistrictPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller for managing astrologer pricing by district
 * Allows different prices for the same astrologer in different districts
 */
@RestController
@RequestMapping("/api/master/astrologer-district-price")
@RequiredArgsConstructor
public class AstrologerDistrictPriceController {

    private final AstrologerDistrictPriceService priceService;

    /**
     * Get price for specific astrologer, district, and puja
     */
    @GetMapping("/get/{astrologerId}/{districtMasterId}/{pujaId}")
    public ResponseEntity<AstrologerDistrictPriceResponse> getPrice(
            @PathVariable Long astrologerId,
            @PathVariable Long districtMasterId,
            @PathVariable Long pujaId) {
        try {
            return ResponseEntity.ok(priceService.getPrice(astrologerId, districtMasterId, pujaId));
        } catch (Exception e) {
            return ResponseEntity.ok(AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Create new price mapping
     */
    @PostMapping("/create")
    public ResponseEntity<AstrologerDistrictPriceResponse> createPrice(
            @Valid @RequestBody AstrologerDistrictPriceRequest request) {
        try {
            return ResponseEntity.ok(priceService.createPrice(request));
        } catch (Exception e) {
            return ResponseEntity.ok(AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Update price
     */
    @PutMapping("/update/{priceId}")
    public ResponseEntity<AstrologerDistrictPriceResponse> updatePrice(
            @PathVariable Long priceId,
            @Valid @RequestBody AstrologerDistrictPriceRequest request) {
        try {
            return ResponseEntity.ok(priceService.updatePrice(priceId, request));
        } catch (Exception e) {
            return ResponseEntity.ok(AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Get all prices for an astrologer
     */
    @GetMapping("/astrologer/{astrologerId}")
    public ResponseEntity<List<AstrologerDistrictPriceResponse>> getPricesByAstrologer(
            @PathVariable Long astrologerId) {
        try {
            return ResponseEntity.ok(priceService.getPricesByAstrologer(astrologerId));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get all prices for a district
     */
    @GetMapping("/district/{districtMasterId}")
    public ResponseEntity<List<AstrologerDistrictPriceResponse>> getPricesByDistrict(
            @PathVariable Long districtMasterId) {
        try {
            return ResponseEntity.ok(priceService.getPricesByDistrict(districtMasterId));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get all prices for a puja
     */
    @GetMapping("/puja/{pujaId}")
    public ResponseEntity<List<AstrologerDistrictPriceResponse>> getPricesByPuja(
            @PathVariable Long pujaId) {
        try {
            return ResponseEntity.ok(priceService.getPricesByPuja(pujaId));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get all prices for astrologer in a specific district
     */
    @GetMapping("/astrologer/{astrologerId}/district/{districtMasterId}")
    public ResponseEntity<List<AstrologerDistrictPriceResponse>> getPricesByAstrologerAndDistrict(
            @PathVariable Long astrologerId,
            @PathVariable Long districtMasterId) {
        try {
            return ResponseEntity.ok(priceService.getPricesByAstrologerAndDistrict(astrologerId, districtMasterId));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Activate/Deactivate price
     */
    @PutMapping("/toggle/{priceId}")
    public ResponseEntity<AstrologerDistrictPriceResponse> togglePriceStatus(
            @PathVariable Long priceId,
            @RequestParam Boolean isActive) {
        try {
            return ResponseEntity.ok(priceService.togglePriceStatus(priceId, isActive));
        } catch (Exception e) {
            return ResponseEntity.ok(AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Delete price
     */
    @DeleteMapping("/delete/{priceId}")
    public ResponseEntity<AstrologerDistrictPriceResponse> deletePrice(@PathVariable Long priceId) {
        try {
            return ResponseEntity.ok(priceService.deletePrice(priceId));
        } catch (Exception e) {
            return ResponseEntity.ok(AstrologerDistrictPriceResponse.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
