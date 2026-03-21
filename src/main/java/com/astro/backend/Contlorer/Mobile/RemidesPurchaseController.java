package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.RemidesPurchaseRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.ResponseDTO.RemidesPurchaseHistoryResponse;
import com.astro.backend.Services.RemidesPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/remides/purchase")
@RequiredArgsConstructor
public class RemidesPurchaseController {

    private final RemidesPurchaseService remidesPurchaseService;

    @PostMapping
    public ResponseEntity<?> purchaseProducts(@RequestBody RemidesPurchaseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(remidesPurchaseService.purchaseCart(request));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> purchaseHistory(
            @PathVariable Long userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<RemidesPurchaseHistoryResponse> items = remidesPurchaseService.getPurchaseHistory(userId);
        final String normalizedSearch = normalizeSearch(search);
        if (!normalizedSearch.isEmpty()) {
            items = items.stream()
                    .filter(item ->
                            contains(item.getOrderId(), normalizedSearch)
                                    || contains(item.getOrderType(), normalizedSearch)
                                    || contains(item.getTitle(), normalizedSearch)
                                    || contains(item.getSubtitle(), normalizedSearch)
                                    || contains(item.getStatus(), normalizedSearch)
                                    || contains(item.getPaymentMethod(), normalizedSearch)
                                    || contains(item.getTransactionId(), normalizedSearch))
                    .toList();
        }

        final boolean pagedRequested = page != null || size != null || !normalizedSearch.isEmpty();
        if (!pagedRequested) {
            return ResponseEntity.ok(items);
        }

        final int pageIndex = normalizePage(page);
        final int pageSize = normalizeSize(size);
        final int total = items.size();
        final int fromIndex = Math.min(pageIndex * pageSize, total);
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<RemidesPurchaseHistoryResponse> pageItems = items.subList(fromIndex, toIndex);
        final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", true);
        payload.put("items", pageItems);
        payload.put("count", total);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("totalPages", totalPages);
        payload.put("hasNext", toIndex < total);
        payload.put("hasPrevious", pageIndex > 0);
        payload.put("search", normalizedSearch);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/history/{userId}/realtime")
    public ResponseEntity<?> purchaseHistoryRealtime(
            @PathVariable Long userId,
            @RequestParam(value = "since", required = false) String since
    ) {
        LocalDateTime sinceTime = null;
        if (since != null && !since.isBlank()) {
            sinceTime = LocalDateTime.parse(since);
        }
        return ResponseEntity.ok(remidesPurchaseService.getPurchaseHistoryRealtime(userId, sinceTime));
    }

    @PostMapping("/history/{orderId}/resend-receipt")
    public ResponseEntity<?> resendReceipt(
            @PathVariable String orderId,
            @Valid @RequestBody ResendReceiptRequest request
    ) {
        try {
            return ResponseEntity.ok(remidesPurchaseService.resendReceiptEmail(orderId, request));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(Object value, String search) {
        if (value == null) {
            return false;
        }
        return value.toString().toLowerCase(Locale.ROOT).contains(search);
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
