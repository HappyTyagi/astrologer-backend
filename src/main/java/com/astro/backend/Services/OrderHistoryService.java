package com.astro.backend.Services;

import com.astro.backend.Entity.OrderHistoryEntry;
import com.astro.backend.Entity.Puja;
import com.astro.backend.Entity.PujaBooking;
import com.astro.backend.Entity.PujaSlot;
import com.astro.backend.Entity.RemidesPurchase;
import com.astro.backend.Repositry.OrderHistoryRepository;
import com.astro.backend.Repositry.PujaBookingRepository;
import com.astro.backend.Repositry.PujaRepository;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.ResponseDTO.RemidesPurchaseHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;
    private final PujaBookingRepository pujaBookingRepository;
    private final PujaRepository pujaRepository;

    @Transactional
    public void recordRemedyPurchases(List<RemidesPurchase> purchases) {
        if (purchases == null || purchases.isEmpty()) return;

        List<OrderHistoryEntry> entries = new ArrayList<>();
        for (RemidesPurchase purchase : purchases) {
            if (purchase == null || purchase.getId() == null) continue;
            if (orderHistoryRepository.existsByOrderTypeAndSourceId("REMEDY", purchase.getId())) {
                continue;
            }

            String subtitle = purchase.getRemides() != null ? purchase.getRemides().getDescription() : null;
            String image = purchase.getRemides() != null ? purchase.getRemides().getImageBase64() : null;

            entries.add(
                    OrderHistoryEntry.builder()
                            .orderId(purchase.getOrderId())
                            .orderType("REMEDY")
                            .userId(purchase.getUserId())
                            .sourceId(purchase.getId())
                            .remidesId(purchase.getRemidesId())
                            .pujaId(null)
                            .title(purchase.getTitle())
                            .subtitle(subtitle)
                            .imageBase64(image)
                            .totalItems(purchase.getQuantity() == null ? 1 : purchase.getQuantity())
                            .unitPrice(purchase.getUnitPrice() == null ? 0.0 : purchase.getUnitPrice())
                            .discountPercentage(purchase.getDiscountPercentage())
                            .finalUnitPrice(purchase.getFinalUnitPrice() == null ? 0.0 : purchase.getFinalUnitPrice())
                            .amount(purchase.getLineTotal() == null ? 0.0 : purchase.getLineTotal())
                            .currency(purchase.getCurrency() == null ? "INR" : purchase.getCurrency())
                            .status(purchase.getStatus() == null ? "COMPLETED" : purchase.getStatus())
                            .purchasedAt(purchase.getPurchasedAt() == null ? LocalDateTime.now() : purchase.getPurchasedAt())
                            .isActive(true)
                            .build()
            );
        }

        if (!entries.isEmpty()) {
            orderHistoryRepository.saveAll(entries);
        }
    }

    @Transactional
    public void recordPujaBooking(PujaBooking booking, Puja puja, PujaSlot slot) {
        if (booking == null || booking.getId() == null) return;
        if (orderHistoryRepository.existsByOrderTypeAndSourceId("PUJA", booking.getId())) {
            return;
        }

        String title = puja != null && puja.getName() != null ? puja.getName() : "Puja Booking";
        String subtitle = "Slot ID: " + (slot != null && slot.getId() != null ? slot.getId() : booking.getSlotId());
        String image = puja != null ? puja.getImage() : null;
        double basePrice = puja != null ? puja.getPrice() : 0.0;
        double total = booking.getTotalPrice() != null ? booking.getTotalPrice() : basePrice;

        OrderHistoryEntry entry = OrderHistoryEntry.builder()
                .orderId("PUJA-" + booking.getId())
                .orderType("PUJA")
                .userId(booking.getUserId())
                .sourceId(booking.getId())
                .remidesId(null)
                .pujaId(booking.getPujaId())
                .title(title)
                .subtitle(subtitle)
                .imageBase64(image)
                .totalItems(1)
                .unitPrice(basePrice)
                .discountPercentage(booking.getDiscountApplied())
                .finalUnitPrice(total)
                .amount(total)
                .currency("INR")
                .status(booking.getStatus() != null ? booking.getStatus().name() : "CONFIRMED")
                .purchasedAt(booking.getBookedAt() != null ? booking.getBookedAt() : LocalDateTime.now())
                .isActive(true)
                .build();
        orderHistoryRepository.save(entry);
    }

    @Transactional
    public List<RemidesPurchaseHistoryResponse> getUserHistory(Long userId) {
        syncMissingHistoryEntriesForUser(userId);
        return orderHistoryRepository.findByUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RemidesPurchaseHistoryResponse> getUserHistorySince(Long userId, LocalDateTime since) {
        syncMissingHistoryEntriesForUser(userId);
        return orderHistoryRepository.findByUserIdAndPurchasedAtAfterOrderByPurchasedAtDesc(userId, since)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RemidesPurchaseHistoryResponse toResponse(OrderHistoryEntry e) {
        return RemidesPurchaseHistoryResponse.builder()
                .id(e.getId())
                .orderId(e.getOrderId())
                .orderType(e.getOrderType())
                .userId(e.getUserId())
                .remidesId(e.getRemidesId())
                .pujaId(e.getPujaId())
                .addressId(null)
                .title(e.getTitle())
                .subtitle(e.getSubtitle())
                .imageBase64(e.getImageBase64())
                .totalItems(e.getTotalItems())
                .unitPrice(e.getUnitPrice())
                .discountPercentage(e.getDiscountPercentage())
                .finalUnitPrice(e.getFinalUnitPrice())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .status(e.getStatus())
                .purchasedAt(e.getPurchasedAt())
                .build();
    }

    private synchronized void syncMissingHistoryEntriesForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }

        List<RemidesPurchase> remedyPurchases = remidesPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId);
        recordRemedyPurchases(remedyPurchases);

        List<PujaBooking> pujaBookings = pujaBookingRepository.findByUserIdOrderByBookedAtDesc(userId);
        if (pujaBookings.isEmpty()) return;

        Set<Long> pujaIds = pujaBookings.stream()
                .map(PujaBooking::getPujaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Puja> pujaMap = pujaRepository.findAllById(pujaIds)
                .stream()
                .collect(Collectors.toMap(Puja::getId, p -> p));

        for (PujaBooking booking : pujaBookings) {
            recordPujaBooking(booking, pujaMap.get(booking.getPujaId()), null);
        }
    }
}
