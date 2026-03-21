package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.Remides;
import com.astro.backend.Entity.RemidesPurchase;
import com.astro.backend.Entity.User;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.WebAdminRemidesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/web/remides")
@RequiredArgsConstructor
public class AdminRemidesController {

    private final RemidesRepository remidesRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> list() {
        List<Remides> all = remidesRepository.findAll();
        List<Remides> active = all.stream()
                .filter(r -> !Boolean.FALSE.equals(r.getIsActive()))
                .toList();
        List<Remides> inactive = all.stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsActive()))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("activeRemedies", active);
        response.put("inactiveRemedies", inactive);
        response.put("countActive", active.size());
        response.put("countInactive", inactive.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getAllRemedyBookings(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "search", required = false) String search
    ) {
        List<RemidesPurchase> purchases = remidesPurchaseRepository.findAllByOrderByPurchasedAtDesc();
        Set<Long> userIds = purchases.stream()
                .map(RemidesPurchase::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<String, List<RemidesPurchase>> byOrder = purchases.stream()
                .collect(Collectors.groupingBy(
                        RemidesPurchase::getOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, List<RemidesPurchase>> entry : byOrder.entrySet()) {
            List<RemidesPurchase> items = entry.getValue();
            items.sort(Comparator.comparing(RemidesPurchase::getPurchasedAt).reversed());

            RemidesPurchase first = items.get(0);
            User user = first.getUserId() == null ? null : userMap.get(first.getUserId());
            Address address = first.getAddress();

            int totalItems = items.stream()
                    .map(RemidesPurchase::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            double totalAmount = items.stream()
                    .map(RemidesPurchase::getLineTotal)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            double fullAmount = items.stream()
                    .map(RemidesPurchase::getFullLineTotal)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", entry.getKey());
            row.put("userId", first.getUserId());
            row.put("userName", user == null ? "Unknown" : user.getName());
            row.put("mobileNumber", user == null ? "" : defaultText(user.getMobileNumber()));
            row.put("email", user == null ? "" : defaultText(user.getEmail()));
            row.put("purchasedAt", first.getPurchasedAt());
            row.put("status", defaultText(first.getStatus()));
            row.put("paymentMethod", defaultText(first.getPaymentMethod()));
            row.put("transactionId", defaultText(first.getTransactionId()));
            row.put("walletUsed", first.getWalletUsed());
            row.put("gatewayPaid", first.getGatewayPaid());
            row.put("currency", defaultText(first.getCurrency()));
            row.put("totalItems", totalItems);
            row.put("totalAmount", totalAmount);
            row.put("fullAmount", fullAmount <= 0.0 ? totalAmount : fullAmount);
            row.put("address", formatAddress(address));
            row.put("titles", items.stream().map(RemidesPurchase::getTitle).toList());
            row.put("lines", items.stream().map(item -> Map.of(
                    "id", item.getId(),
                    "remidesId", item.getRemidesId(),
                    "title", defaultText(item.getTitle()),
                    "quantity", item.getQuantity() == null ? 0 : item.getQuantity(),
                    "lineTotal", item.getLineTotal() == null ? 0.0 : item.getLineTotal(),
                    "fullLineTotal", item.getFullLineTotal() == null ? 0.0 : item.getFullLineTotal(),
                    "tokenUnitAmount", item.getTokenUnitAmount() == null ? 0.0 : item.getTokenUnitAmount()
            )).toList());
            rows.add(row);
        }

        final String normalizedSearch = normalizeSearch(search);
        final List<Map<String, Object>> filteredRows = rows.stream()
                .filter(row -> matchesSearch(row, normalizedSearch))
                .toList();

        final boolean pagedRequested = page != null || size != null || !normalizedSearch.isEmpty();
        if (!pagedRequested) {
            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "count", filteredRows.size(),
                    "bookings", filteredRows
            ));
        }

        final int pageIndex = normalizePage(page);
        final int pageSize = normalizeSize(size);
        final int total = filteredRows.size();
        final int fromIndex = Math.min(pageIndex * pageSize, total);
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<Map<String, Object>> pageItems = filteredRows.subList(fromIndex, toIndex);
        final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", true);
        payload.put("count", total);
        payload.put("bookings", pageItems);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("totalPages", totalPages);
        payload.put("hasNext", toIndex < total);
        payload.put("hasPrevious", pageIndex > 0);
        payload.put("search", normalizedSearch);
        return ResponseEntity.ok(payload);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody WebAdminRemidesRequest request) {
        Remides remides = Remides.builder()
                .userId(request.getUserId())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .price(request.getPrice())
                .tokenAmount(request.getTokenAmount())
                .discountPercentage(request.getDiscountPercentage())
                .currency(request.getCurrency().trim())
                .imageBase64(request.getImageBase64())
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .build();
        Remides saved = remidesRepository.save(remides);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody WebAdminRemidesRequest request) {
        Remides existing = remidesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remides not found: " + id));

        existing.setUserId(request.getUserId());
        existing.setTitle(request.getTitle().trim());
        existing.setDescription(request.getDescription().trim());
        existing.setPrice(request.getPrice());
        existing.setTokenAmount(request.getTokenAmount());
        existing.setDiscountPercentage(request.getDiscountPercentage());
        existing.setCurrency(request.getCurrency().trim());
        existing.setImageBase64(request.getImageBase64());
        existing.setIsActive(request.getIsActive() == null ? true : request.getIsActive());

        return ResponseEntity.ok(remidesRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Remides existing = remidesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remides not found: " + id));
        existing.setIsActive(false);
        remidesRepository.save(existing);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", true);
        resp.put("message", "Remides deleted successfully");
        resp.put("id", id);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        Remides existing = remidesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remides not found: " + id));
        existing.setIsActive(true);
        remidesRepository.save(existing);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", true);
        resp.put("message", "Remides activated successfully");
        resp.put("id", id);
        return ResponseEntity.ok(resp);
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "";
        }
        return String.join(
                ", ",
                List.of(
                        defaultText(address.getName()),
                        defaultText(address.getAddressLine1()),
                        defaultText(address.getAddressLine2()),
                        defaultText(address.getCity()),
                        defaultText(address.getDistrict()),
                        defaultText(address.getState()),
                        defaultText(address.getPincode())
                ).stream().filter(value -> !value.isBlank()).toList()
        );
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(Map<String, Object> row, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        return contains(row.get("orderId"), search)
                || contains(row.get("userName"), search)
                || contains(row.get("mobileNumber"), search)
                || contains(row.get("email"), search)
                || contains(row.get("status"), search)
                || contains(row.get("paymentMethod"), search)
                || contains(row.get("transactionId"), search)
                || contains(row.get("address"), search)
                || contains(row.get("titles"), search);
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
