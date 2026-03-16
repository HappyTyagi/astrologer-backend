package com.astro.backend.Services;

import com.astro.backend.Entity.PujaSamagriCart;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Repositry.PujaSamagriCartRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.RequestDTO.PujaSamagriCartSyncRequest;
import com.astro.backend.ResponseDTO.PujaSamagriCartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PujaSamagriCartService {

    private static final int MAX_QTY = 10;

    private final PujaSamagriCartRepository cartRepository;
    private final PujaSamagriMasterRepository masterRepository;

    @Transactional
    public Map<String, Object> syncCart(PujaSamagriCartSyncRequest request) {
        validateRequest(request);

        final Long userId = request.getUserId();
        final List<PujaSamagriCartSyncRequest.CartItem> requestedItems =
                request.getItems() == null ? Collections.emptyList() : request.getItems();

        final Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (PujaSamagriCartSyncRequest.CartItem item : requestedItems) {
            if (item == null || item.getSamagriMasterId() == null) {
                continue;
            }
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            if (qty < 1) {
                continue;
            }
            if (qty > MAX_QTY) {
                qty = MAX_QTY;
            }
            normalized.put(item.getSamagriMasterId(), qty);
        }

        final List<PujaSamagriCart> existing = cartRepository.findByUserId(userId);
        final Map<Long, PujaSamagriCart> existingByMasterId = existing.stream()
                .filter(e -> e.getSamagriMaster() != null && e.getSamagriMaster().getId() != null)
                .collect(Collectors.toMap(e -> e.getSamagriMaster().getId(), e -> e, (a, b) -> a));

        final LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Integer> entry : normalized.entrySet()) {
            final Long masterId = entry.getKey();
            final Integer qty = entry.getValue();

            PujaSamagriCart cart = existingByMasterId.get(masterId);
            if (cart == null) {
                PujaSamagriMaster master = masterRepository.findById(masterId)
                        .orElseThrow(() -> new RuntimeException("Samagri item not found: " + masterId));
                cart = PujaSamagriCart.builder()
                        .userId(userId)
                        .samagriMaster(master)
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
            cartRepository.save(cart);
        }

        for (PujaSamagriCart cart : existing) {
            final Long masterId = cart.getSamagriMaster() == null ? null : cart.getSamagriMaster().getId();
            if (masterId == null) {
                continue;
            }
            if (!normalized.containsKey(masterId) && Boolean.TRUE.equals(cart.getIsActive())) {
                cart.setIsActive(false);
                cart.setUpdatedAt(now);
                cartRepository.save(cart);
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
    public Map<String, Object> removeItem(Long userId, Long samagriMasterId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (samagriMasterId == null || samagriMasterId <= 0) {
            throw new RuntimeException("Valid samagriMasterId is required");
        }

        cartRepository.findByUserIdAndSamagriMaster_IdAndIsActiveTrue(userId, samagriMasterId)
                .ifPresent(cart -> {
                    cart.setIsActive(false);
                    cartRepository.save(cart);
                });

        return buildCartResponse(userId, "Item removed from cart");
    }

    @Transactional
    public Map<String, Object> clearCart(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final List<PujaSamagriCart> userItems = cartRepository.findByUserId(userId);
        userItems.forEach(item -> item.setIsActive(false));
        cartRepository.saveAll(userItems);
        return buildCartResponse(userId, "Cart cleared successfully");
    }

    private Map<String, Object> buildCartResponse(Long userId, String message) {
        final List<PujaSamagriCartItemResponse> items = cartRepository
                .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        final int totalItems = items.stream()
                .map(PujaSamagriCartItemResponse::getQuantity)
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
        response.put("payableAmount", totalAmount);
        response.put("items", items);
        response.put("serverTime", LocalDateTime.now());
        return response;
    }

    private PujaSamagriCartItemResponse toResponse(PujaSamagriCart cart) {
        PujaSamagriMaster master = cart.getSamagriMaster();
        return PujaSamagriCartItemResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .samagriMasterId(master != null ? master.getId() : null)
                .quantity(cart.getQuantity())
                .name(master != null ? master.getName() : null)
                .hiName(master != null ? master.getHiName() : null)
                .description(master != null ? master.getDescription() : null)
                .hiDescription(master != null ? master.getHiDescription() : null)
                .imageUrl(master != null ? master.getImageUrl() : null)
                .price(master != null ? master.getPrice() : null)
                .discountPercentage(master != null ? master.getDiscountPercentage() : null)
                .finalPrice(master != null ? master.getFinalPrice() : null)
                .currency(master != null ? master.getCurrency() : "INR")
                .build();
    }

    private void validateRequest(PujaSamagriCartSyncRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
    }
}
