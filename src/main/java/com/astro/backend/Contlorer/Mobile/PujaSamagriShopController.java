package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.PujaSamagriMasterImage;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Repositry.PujaSamagriMasterImageRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.ResponseDTO.PujaSamagriShopItemResponse;
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
    public ResponseEntity<?> listShopItems() {
        List<PujaSamagriMaster> masters = masterRepository.findByIsActiveOrderByName(true)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getShopEnabled()))
                .toList();
        if (masters.isEmpty()) {
            return ResponseEntity.ok(List.of());
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
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(@PathVariable Long id) {
        PujaSamagriMaster item = masterRepository.findById(id).orElse(null);
        if (item == null || Boolean.FALSE.equals(item.getIsActive()) || Boolean.FALSE.equals(item.getShopEnabled())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", false,
                            "message", "Samagri item not found"
                    ));
        }
        final List<String> images = imageRepository
                .findBySamagriMaster_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .map(PujaSamagriMasterImage::getImageUrl)
                .toList();

        return ResponseEntity.ok(toShopResponse(item, images));
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
