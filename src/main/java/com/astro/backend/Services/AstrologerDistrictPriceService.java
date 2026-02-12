package com.astro.backend.Services;

import com.astro.backend.Entity.AstrologerDistrictPrice;
import com.astro.backend.Repositry.AstrologerDistrictPriceRepository;
import com.astro.backend.RequestDTO.AstrologerDistrictPriceRequest;
import com.astro.backend.ResponseDTO.AstrologerDistrictPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AstrologerDistrictPriceService {

    private final AstrologerDistrictPriceRepository priceRepository;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get current price for astrologer in a specific district and puja
     */
    public AstrologerDistrictPriceResponse getPrice(Long astrologerId, Long districtMasterId, Long pujaId) {
        var price = priceRepository.findActivePrice(astrologerId, districtMasterId, pujaId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("No active price found for the given criteria"));

        return mapToResponse(price, true, "Price fetched successfully");
    }

    /**
     * Get current price by location for mobile flow.
     * If astrologerId is provided, exact match is preferred.
     */
    public AstrologerDistrictPriceResponse getLocationBasedPrice(Long pujaId, Long districtMasterId, Long astrologerId) {
        LocalDateTime now = LocalDateTime.now();
        AstrologerDistrictPrice selectedPrice = null;

        if (astrologerId != null) {
            selectedPrice = priceRepository
                    .findActivePrice(astrologerId, districtMasterId, pujaId, now)
                    .orElse(null);
        }

        if (selectedPrice == null) {
            List<AstrologerDistrictPrice> priceList =
                    priceRepository.findActivePricesByDistrictAndPuja(districtMasterId, pujaId, now);
            if (!priceList.isEmpty()) {
                selectedPrice = priceList.get(0);
            }
        }

        if (selectedPrice == null) {
            throw new RuntimeException("No active location-based price found");
        }

        return mapToResponse(selectedPrice, true, "Location-based price fetched successfully");
    }

    /**
     * Create new astrologer district price mapping
     */
    public AstrologerDistrictPriceResponse createPrice(AstrologerDistrictPriceRequest request) {
        // Check if already exists
        if (priceRepository.existsByAstrologerIdAndDistrictMasterIdAndPujaId(
                request.getAstrologerId(), request.getDistrictMasterId(), request.getPujaId())) {
            throw new RuntimeException("Price mapping already exists for this astrologer-district-puja combination");
        }

        AstrologerDistrictPrice price = AstrologerDistrictPrice.builder()
                .astrologerId(request.getAstrologerId())
                .districtMasterId(request.getDistrictMasterId())
                .pujaId(request.getPujaId())
                .consultationPrice(request.getConsultationPrice())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0)
                .notes(request.getNotes())
                .minBookings(request.getMinBookings() != null ? request.getMinBookings() : 0)
                .maxCapacity(request.getMaxCapacity())
                .validFrom(request.getValidFrom() != null ? LocalDateTime.parse(request.getValidFrom(), dateTimeFormatter) : LocalDateTime.now())
                .validTill(request.getValidTill() != null ? LocalDateTime.parse(request.getValidTill(), dateTimeFormatter) : null)
                .isActive(true)
                .build();

        AstrologerDistrictPrice savedPrice = priceRepository.save(price);
        return mapToResponse(savedPrice, true, "Price created successfully");
    }

    /**
     * Update existing price
     */
    public AstrologerDistrictPriceResponse updatePrice(Long priceId, AstrologerDistrictPriceRequest request) {
        AstrologerDistrictPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new RuntimeException("Price not found with ID: " + priceId));

        price.setConsultationPrice(request.getConsultationPrice());
        price.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0);
        price.setNotes(request.getNotes());
        price.setMinBookings(request.getMinBookings() != null ? request.getMinBookings() : 0);
        price.setMaxCapacity(request.getMaxCapacity());
        price.setValidTill(request.getValidTill() != null ? LocalDateTime.parse(request.getValidTill(), dateTimeFormatter) : null);

        AstrologerDistrictPrice updatedPrice = priceRepository.save(price);
        return mapToResponse(updatedPrice, true, "Price updated successfully");
    }

    /**
     * Get all prices for an astrologer
     */
    public List<AstrologerDistrictPriceResponse> getPricesByAstrologer(Long astrologerId) {
        List<AstrologerDistrictPrice> prices = priceRepository.findByAstrologerIdAndIsActiveTrue(astrologerId);
        return prices.stream()
                .map(p -> mapToResponse(p, true, ""))
                .collect(Collectors.toList());
    }

    /**
     * Get all prices for a district
     */
    public List<AstrologerDistrictPriceResponse> getPricesByDistrict(Long districtMasterId) {
        List<AstrologerDistrictPrice> prices = priceRepository.findByDistrictMasterIdAndIsActiveTrue(districtMasterId);
        return prices.stream()
                .map(p -> mapToResponse(p, true, ""))
                .collect(Collectors.toList());
    }

    /**
     * Get all prices for a puja
     */
    public List<AstrologerDistrictPriceResponse> getPricesByPuja(Long pujaId) {
        List<AstrologerDistrictPrice> prices = priceRepository.findByPujaIdAndIsActiveTrue(pujaId);
        return prices.stream()
                .map(p -> mapToResponse(p, true, ""))
                .collect(Collectors.toList());
    }

    /**
     * Get all prices for astrologer in specific district
     */
    public List<AstrologerDistrictPriceResponse> getPricesByAstrologerAndDistrict(Long astrologerId, Long districtMasterId) {
        List<AstrologerDistrictPrice> prices = priceRepository.findByAstrologerIdAndDistrictMasterIdAndIsActiveTrue(astrologerId, districtMasterId);
        return prices.stream()
                .map(p -> mapToResponse(p, true, ""))
                .collect(Collectors.toList());
    }

    /**
     * Activate/Deactivate price
     */
    public AstrologerDistrictPriceResponse togglePriceStatus(Long priceId, Boolean isActive) {
        AstrologerDistrictPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new RuntimeException("Price not found with ID: " + priceId));

        price.setIsActive(isActive);
        AstrologerDistrictPrice updatedPrice = priceRepository.save(price);
        return mapToResponse(updatedPrice, true, "Price status updated successfully");
    }

    /**
     * Delete price
     */
    public AstrologerDistrictPriceResponse deletePrice(Long priceId) {
        AstrologerDistrictPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new RuntimeException("Price not found with ID: " + priceId));

        priceRepository.deleteById(priceId);
        return mapToResponse(price, true, "Price deleted successfully");
    }

    /**
     * Map entity to response
     */
    private AstrologerDistrictPriceResponse mapToResponse(AstrologerDistrictPrice price, Boolean status, String message) {
        return AstrologerDistrictPriceResponse.builder()
                .id(price.getId())
                .astrologerId(price.getAstrologerId())
                .districtMasterId(price.getDistrictMasterId())
                .pujaId(price.getPujaId())
                .consultationPrice(price.getConsultationPrice())
                .discountPercentage(price.getDiscountPercentage())
                .finalPrice(price.getFinalPrice())
                .isActive(price.getIsActive())
                .notes(price.getNotes())
                .minBookings(price.getMinBookings())
                .maxCapacity(price.getMaxCapacity())
                .validFrom(price.getValidFrom())
                .validTill(price.getValidTill())
                .createdAt(price.getCreatedAt())
                .updatedAt(price.getUpdatedAt())
                .status(status)
                .message(message)
                .build();
    }
}
