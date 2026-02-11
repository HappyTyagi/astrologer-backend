package com.astro.backend.Services;

import com.astro.backend.Entity.Remides;
import com.astro.backend.Entity.RemidesCart;
import com.astro.backend.Repositry.RemidesCartRepository;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.RequestDTO.RemidesCartSyncRequest;
import com.astro.backend.ResponseDTO.RemidesCartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemidesCartService {

    private static final int MAX_QTY = 10;

    private final RemidesCartRepository remidesCartRepository;
    private final RemidesRepository remidesRepository;

    @Transactional
    public Map<String, Object> syncCart(RemidesCartSyncRequest request) {
        validateRequest(request);

        final Long userId = request.getUserId();
        final List<RemidesCartSyncRequest.CartItem> requestedItems =
                request.getItems() == null ? Collections.emptyList() : request.getItems();

        final Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (RemidesCartSyncRequest.CartItem item : requestedItems) {
            if (item == null || item.getRemidesId() == null) {
                continue;
            }
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            if (qty < 1) {
                continue;
            }
            if (qty > MAX_QTY) {
                qty = MAX_QTY;
            }
            normalized.put(item.getRemidesId(), qty);
        }

        final List<RemidesCart> existing = remidesCartRepository.findByUserId(userId);
        final Map<Long, RemidesCart> existingByRemidesId = existing.stream()
                .filter(e -> e.getRemides() != null && e.getRemides().getId() != null)
                .collect(Collectors.toMap(e -> e.getRemides().getId(), e -> e, (a, b) -> a));

        final LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Integer> entry : normalized.entrySet()) {
            final Long remidesId = entry.getKey();
            final Integer qty = entry.getValue();

            RemidesCart cart = existingByRemidesId.get(remidesId);
            if (cart == null) {
                Remides remides = remidesRepository.findById(remidesId)
                        .orElseThrow(() -> new RuntimeException("Remides not found: " + remidesId));
                cart = RemidesCart.builder()
                        .userId(userId)
                        .remides(remides)
                        .quantity(qty)
                        .createdAt(now)
                        .updatedAt(now)
                        .isActive(true)
                        .build();
            } else {
                cart.setQuantity(qty);
                cart.setIsActive(true);
                cart.setUpdatedAt(now);
            }
            remidesCartRepository.save(cart);
        }

        // Soft-remove any items not present in client cart snapshot.
        for (RemidesCart cart : existing) {
            final Long remidesId = cart.getRemides() == null ? null : cart.getRemides().getId();
            if (remidesId == null) {
                continue;
            }
            if (!normalized.containsKey(remidesId) && Boolean.TRUE.equals(cart.getIsActive())) {
                cart.setIsActive(false);
                cart.setUpdatedAt(now);
                remidesCartRepository.save(cart);
            }
        }

        return buildCartResponse(userId, "Cart synced successfully");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCart(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        return buildCartResponse(userId, "Cart fetched successfully");
    }

    @Transactional
    public Map<String, Object> removeItem(Long userId, Long remidesId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (remidesId == null || remidesId <= 0) {
            throw new RuntimeException("Valid remidesId is required");
        }

        remidesCartRepository.findByUserIdAndRemides_IdAndIsActiveTrue(userId, remidesId)
                .ifPresent(cart -> {
                    cart.setIsActive(false);
                    remidesCartRepository.save(cart);
                });

        return buildCartResponse(userId, "Item removed from cart");
    }

    @Transactional
    public Map<String, Object> clearCart(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final List<RemidesCart> userItems = remidesCartRepository.findByUserId(userId);
        userItems.forEach(item -> item.setIsActive(false));
        remidesCartRepository.saveAll(userItems);
        return buildCartResponse(userId, "Cart cleared successfully");
    }

    private Map<String, Object> buildCartResponse(Long userId, String message) {
        final List<RemidesCartItemResponse> items = remidesCartRepository
                .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        final int totalItems = items.stream()
                .map(RemidesCartItemResponse::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        final double totalAmount = items.stream()
                .mapToDouble(item -> {
                    double unit = item.getFinalPrice() == null
                            ? (item.getPrice() == null ? 0.0 : item.getPrice())
                            : item.getFinalPrice();
                    int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                    return unit * qty;
                })
                .sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("totalItems", totalItems);
        response.put("totalAmount", totalAmount);
        response.put("items", items);
        response.put("serverTime", LocalDateTime.now());
        return response;
    }

    private RemidesCartItemResponse toResponse(RemidesCart cart) {
        Remides remides = cart.getRemides();
        return RemidesCartItemResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .remidesId(remides != null ? remides.getId() : null)
                .quantity(cart.getQuantity())
                .title(remides != null ? remides.getTitle() : null)
                .subtitle(remides != null ? remides.getDescription() : null)
                .imageBase64(remides != null ? remides.getImageBase64() : null)
                .price(remides != null ? remides.getPrice() : null)
                .discountPercentage(remides != null ? remides.getDiscountPercentage() : null)
                .finalPrice(remides != null ? remides.getFinalPrice() : null)
                .currency(remides != null ? remides.getCurrency() : "INR")
                .build();
    }

    private void validateRequest(RemidesCartSyncRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
    }
}
