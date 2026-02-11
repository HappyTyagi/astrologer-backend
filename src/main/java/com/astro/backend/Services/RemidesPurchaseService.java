package com.astro.backend.Services;

import com.astro.backend.Entity.Remides;
import com.astro.backend.Entity.RemidesPurchase;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.RequestDTO.RemidesPurchaseRequest;
import com.astro.backend.ResponseDTO.RemidesPurchaseHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RemidesPurchaseService {

    private final RemidesRepository remidesRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;

    @Transactional
    public Map<String, Object> purchaseCart(RemidesPurchaseRequest request) {
        validateRequest(request);

        final String orderId = UUID.randomUUID().toString();
        final LocalDateTime purchaseTime = LocalDateTime.now();

        final List<RemidesPurchase> purchases = new ArrayList<>();
        double totalAmount = 0.0;
        int totalQuantity = 0;

        for (RemidesPurchaseRequest.PurchaseItem item : request.getItems()) {
            Remides remides = remidesRepository.findById(item.getRemidesId())
                    .orElseThrow(() -> new RuntimeException("Remides not found: " + item.getRemidesId()));

            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            double unitPrice = remides.getPrice() == null ? 0.0 : remides.getPrice();
            double finalUnitPrice = remides.getFinalPrice() == null ? unitPrice : remides.getFinalPrice();
            double lineTotal = finalUnitPrice * quantity;

            totalAmount += lineTotal;
            totalQuantity += quantity;

            purchases.add(RemidesPurchase.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .remides(remides)
                    .title(remides.getTitle())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountPercentage(remides.getDiscountPercentage())
                    .finalUnitPrice(finalUnitPrice)
                    .lineTotal(lineTotal)
                    .currency(remides.getCurrency())
                    .status("WAITING")
                    .purchasedAt(purchaseTime)
                    .build());
        }

        List<RemidesPurchase> saved = remidesPurchaseRepository.saveAll(purchases);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Products purchased successfully");
        response.put("orderId", orderId);
        response.put("userId", request.getUserId());
        response.put("totalItems", totalQuantity);
        response.put("totalAmount", totalAmount);
        response.put("purchasedAt", purchaseTime);
        response.put("purchases", saved);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RemidesPurchaseHistoryResponse> getPurchaseHistory(Long userId) {
        List<RemidesPurchase> purchases =
                remidesPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId);
        return purchases.stream().map(this::toHistoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPurchaseHistoryRealtime(Long userId, LocalDateTime since) {
        List<RemidesPurchase> purchases = since == null
                ? remidesPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId)
                : remidesPurchaseRepository.findByUserIdAndPurchasedAtAfterOrderByPurchasedAtDesc(userId, since);

        List<RemidesPurchaseHistoryResponse> data =
                purchases.stream().map(this::toHistoryResponse).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("serverTime", LocalDateTime.now());
        response.put("count", data.size());
        response.put("items", data);
        return response;
    }

    private void validateRequest(RemidesPurchaseRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }
    }

    private RemidesPurchaseHistoryResponse toHistoryResponse(RemidesPurchase purchase) {
        String subtitle = purchase.getRemides() != null
                ? purchase.getRemides().getDescription()
                : null;
        String imageBase64 = purchase.getRemides() != null
                ? purchase.getRemides().getImageBase64()
                : null;

        return RemidesPurchaseHistoryResponse.builder()
                .id(purchase.getId())
                .orderId(purchase.getOrderId())
                .userId(purchase.getUserId())
                .remidesId(purchase.getRemidesId())
                .title(purchase.getTitle())
                .subtitle(subtitle)
                .imageBase64(imageBase64)
                .totalItems(purchase.getQuantity())
                .unitPrice(purchase.getUnitPrice())
                .discountPercentage(purchase.getDiscountPercentage())
                .finalUnitPrice(purchase.getFinalUnitPrice())
                .amount(purchase.getLineTotal())
                .currency(purchase.getCurrency())
                .status(purchase.getStatus())
                .purchasedAt(purchase.getPurchasedAt())
                .build();
    }
}
