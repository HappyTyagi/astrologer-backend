package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.GotraMaster;
import com.astro.backend.Repositry.GotraMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/master/gotra")
@RequiredArgsConstructor
public class GotraMasterController {

    private final GotraMasterRepository gotraMasterRepository;

    @GetMapping("/all")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(gotraMasterRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        return ResponseEntity.ok(gotraMasterRepository.findByIsActiveOrderByName(true));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") == null ? "" : payload.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Gotra name is required");
        }
        String description = payload.get("description") == null ? null : payload.get("description").toString().trim();
        GotraMaster saved = gotraMasterRepository.save(
                GotraMaster.builder()
                        .name(name)
                        .description(description)
                        .isActive(true)
                        .build()
        );
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gotra inserted successfully",
                "item", saved
        ));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        GotraMaster existing = gotraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gotra not found: " + id));
        if (payload.containsKey("name") && payload.get("name") != null) {
            String name = payload.get("name").toString().trim();
            if (name.isEmpty()) {
                throw new RuntimeException("Gotra name is required");
            }
            existing.setName(name);
        }
        if (payload.containsKey("description")) {
            existing.setDescription(payload.get("description") == null ? null : payload.get("description").toString().trim());
        }
        if (payload.containsKey("isActive")) {
            Object raw = payload.get("isActive");
            if (raw instanceof Boolean flag) {
                existing.setIsActive(flag);
            }
        }
        GotraMaster saved = gotraMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gotra updated successfully",
                "item", saved
        ));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        GotraMaster existing = gotraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gotra not found: " + id));
        existing.setIsActive(false);
        gotraMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gotra deleted successfully",
                "id", id
        ));
    }
}
