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
    public ResponseEntity<ApiResponse<Object>> listShopItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        final List<PujaSamagriMaster> activeMasters = masterRepository.findByIsActiveOrderByName(true);
        List<PujaSamagriMaster> masters = activeMasters.stream()
                .filter(m -> Boolean.TRUE.equals(m.getShopEnabled()))
                .toList();
        if (masters.isEmpty()) {
            // Backward compatibility: if shopEnabled isn't configured yet, show all active items.
            masters = activeMasters;
        }
        final String normalizedSearch = normalizeSearch(search);
        if (!normalizedSearch.isEmpty()) {
            masters = masters.stream()
                    .filter(master ->
                            containsSearch(master.getName(), normalizedSearch)
                                    || containsSearch(master.getHiName(), normalizedSearch)
                                    || containsSearch(master.getDescription(), normalizedSearch)
                                    || containsSearch(master.getHiDescription(), normalizedSearch))
                    .toList();
        }

        final List<Long> ids = masters.stream()
                .map(PujaSamagriMaster::getId)
                .filter(Objects::nonNull)
                .toList();

        final Map<Long, List<String>> imagesByMasterId = ids.isEmpty()
                ? Map.of()
                : imageRepository.findActiveByMasterIdsOrdered(ids)
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

        final boolean pagedRequested = page != null || size != null || !normalizedSearch.isEmpty();
        if (!pagedRequested) {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            response,
                            true,
                            response.isEmpty() ? "No items available" : "Shop items fetched successfully"
                    )
            );
        }

        final int pageIndex = normalizePage(page);
        final int pageSize = normalizeSize(size);
        final int total = response.size();
        final int fromIndex = Math.min(pageIndex * pageSize, total);
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<PujaSamagriShopItemResponse> pageItems = response.subList(fromIndex, toIndex);
        final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", pageItems);
        payload.put("count", total);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("totalPages", totalPages);
        payload.put("hasNext", toIndex < total);
        payload.put("hasPrevious", pageIndex > 0);
        payload.put("search", normalizedSearch);

        return ResponseEntity.ok(
                ResponseUtils.createSuccessResponse(
                        payload,
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

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim().toLowerCase();
    }

    private boolean containsSearch(String value, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase().contains(search);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 12;
        }
        return Math.min(size, 100);
    }
}
