package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.PujaSamagriMasterImage;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Repositry.PujaSamagriMasterImageRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.ResponseDTO.PujaSamagriShopItemResponse;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/puja-samagri")
@RequiredArgsConstructor
public class PujaSamagriShopController {

    private final PujaSamagriMasterRepository masterRepository;
    private final PujaSamagriMasterImageRepository imageRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PujaSamagriShopItemResponse>>> listShopItems() {
        final List<PujaSamagriMaster> activeMasters = masterRepository.findByIsActiveOrderByName(true);
        List<PujaSamagriMaster> masters = activeMasters.stream()
                .filter(m -> Boolean.TRUE.equals(m.getShopEnabled()))
                .toList();
        if (masters.isEmpty()) {
            // Backward compatibility: if shopEnabled isn't configured yet, show all active items.
            masters = activeMasters;
        }
        if (masters.isEmpty()) {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            List.of(),
                            true,
                            "No items available"
                    )
            );
        }

        final List<Long> ids = masters.stream()
                .map(PujaSamagriMaster::getId)
                .filter(Objects::nonNull)
                .toList();

        final Map<Long, List<String>> imagesByMasterId = imageRepository.findActiveByMasterIdsOrdered(ids)
                .stream()
                .filter(i -> i.getSamagriMaster() != null && i.getSamagriMaster().getId() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getSamagriMaster().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(PujaSamagriMasterImage::getImageUrl, Collectors.toList())
                ));

        final List<PujaSamagriShopItemResponse> response = masters.stream()
                .map(m -> toShopResponse(m, imagesByMasterId.get(m.getId())))
                .toList();
        return ResponseEntity.ok(
                ResponseUtils.createSuccessResponse(
                        response,
                        true,
                        "Shop items fetched successfully"
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PujaSamagriShopItemResponse>> getItem(@PathVariable Long id) {
        PujaSamagriMaster item = masterRepository.findById(id).orElse(null);
        if (item == null || Boolean.FALSE.equals(item.getIsActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtils.createFailureResponse(
                            "Samagri item not found",
                            false,
                            "SAMAGRI_NOT_FOUND"
                    ));
        }
        final List<String> images = imageRepository
                .findBySamagriMaster_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .map(PujaSamagriMasterImage::getImageUrl)
                .toList();

        return ResponseEntity.ok(
                ResponseUtils.createSuccessResponse(
                        toShopResponse(item, images),
                        true,
                        "Shop item fetched successfully"
                )
        );
    }

    private PujaSamagriShopItemResponse toShopResponse(PujaSamagriMaster master, List<String> images) {
        final List<String> normalized = mergeImages(master == null ? null : master.getImageUrl(), images);
        return PujaSamagriShopItemResponse.builder()
                .id(master == null ? null : master.getId())
                .name(master == null ? null : master.getName())
                .hiName(master == null ? null : master.getHiName())
                .description(master == null ? null : master.getDescription())
                .hiDescription(master == null ? null : master.getHiDescription())
                .price(master == null ? null : master.getPrice())
                .discountPercentage(master == null ? null : master.getDiscountPercentage())
                .finalPrice(master == null ? null : master.getFinalPrice())
                .currency(master == null ? null : master.getCurrency())
                .imageUrl(master == null ? null : master.getImageUrl())
                .images(normalized)
                .shopEnabled(master == null ? null : master.getShopEnabled())
                .build();
    }

    private List<String> mergeImages(String primary, List<String> images) {
        final LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (primary != null) {
            final String trimmed = primary.trim();
            if (!trimmed.isEmpty()) merged.add(trimmed);
        }
        if (images != null) {
            for (String url : images) {
                if (url == null) continue;
                final String trimmed = url.trim();
                if (!trimmed.isEmpty()) merged.add(trimmed);
            }
        }
        return merged.stream().toList();
    }
}
