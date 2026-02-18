package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.YantraMaster;
import com.astro.backend.Repositry.YantraMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/master/yantra")
@RequiredArgsConstructor
public class YantraMasterController {

    private final YantraMasterRepository yantraMasterRepository;

    @GetMapping("/all")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(yantraMasterRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        return ResponseEntity.ok(yantraMasterRepository.findByIsActiveOrderByName(true));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") == null ? "" : payload.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Yantra name is required");
        }
        String deity = payload.get("associatedDeity") == null ? null : payload.get("associatedDeity").toString().trim();
        String description = payload.get("description") == null ? null : payload.get("description").toString().trim();
        YantraMaster saved = yantraMasterRepository.save(
                YantraMaster.builder()
                        .name(name)
                        .associatedDeity(deity)
                        .description(description)
                        .isActive(true)
                        .build()
        );
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Yantra inserted successfully",
                "item", saved
        ));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        YantraMaster existing = yantraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Yantra not found: " + id));
        if (payload.containsKey("name") && payload.get("name") != null) {
            String name = payload.get("name").toString().trim();
            if (!name.isEmpty()) {
                existing.setName(name);
            }
        }
        if (payload.containsKey("associatedDeity")) {
            existing.setAssociatedDeity(payload.get("associatedDeity") == null ? null : payload.get("associatedDeity").toString().trim());
        }
        if (payload.containsKey("description")) {
            existing.setDescription(payload.get("description") == null ? null : payload.get("description").toString().trim());
        }
        if (payload.containsKey("isActive") && payload.get("isActive") instanceof Boolean flag) {
            existing.setIsActive(flag);
        }
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Yantra updated successfully",
                "item", yantraMasterRepository.save(existing)
        ));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        YantraMaster existing = yantraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Yantra not found: " + id));
        existing.setIsActive(false);
        yantraMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Yantra deleted successfully",
                "id", id
        ));
    }
}
