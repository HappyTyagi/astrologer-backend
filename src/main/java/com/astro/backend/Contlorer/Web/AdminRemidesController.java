package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.Remides;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.RequestDTO.WebAdminRemidesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/remides")
@RequiredArgsConstructor
public class AdminRemidesController {

    private final RemidesRepository remidesRepository;

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
}
