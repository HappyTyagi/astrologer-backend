package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.RashiMaster;
import com.astro.backend.Repositry.RashiMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/master/rashi")
@RequiredArgsConstructor
public class RashiMasterController {

    private final RashiMasterRepository rashiMasterRepository;

    @GetMapping("/all")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(rashiMasterRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        return ResponseEntity.ok(rashiMasterRepository.findByIsActiveOrderByName(true));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") == null ? "" : payload.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Rashi name is required");
        }
        String image = parseImage(payload);
        String description = payload.get("description") == null ? null : payload.get("description").toString().trim();
        RashiMaster saved = rashiMasterRepository.save(
                RashiMaster.builder()
                        .name(name)
                        .image(image)
                        .description(description)
                        .isActive(true)
                        .build()
        );
        return ResponseEntity.ok(Map.of("status", true, "message", "Rashi inserted successfully", "item", saved));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        RashiMaster existing = rashiMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rashi not found: " + id));
        if (payload.containsKey("name") && payload.get("name") != null) {
            String name = payload.get("name").toString().trim();
            if (name.isEmpty()) {
                throw new RuntimeException("Rashi name is required");
            }
            existing.setName(name);
        }
        if (payload.containsKey("description")) {
            existing.setDescription(payload.get("description") == null ? null : payload.get("description").toString().trim());
        }
        if (payload.containsKey("image") || payload.containsKey("imageBase64")) {
            existing.setImage(parseImage(payload));
        }
        if (payload.containsKey("isActive") && payload.get("isActive") instanceof Boolean flag) {
            existing.setIsActive(flag);
        }
        RashiMaster saved = rashiMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of("status", true, "message", "Rashi updated successfully", "item", saved));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        RashiMaster existing = rashiMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rashi not found: " + id));
        existing.setIsActive(false);
        rashiMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of("status", true, "message", "Rashi deleted successfully", "id", id));
    }

    private String parseImage(Map<String, Object> payload) {
        Object value = payload.containsKey("image") ? payload.get("image") : payload.get("imageBase64");
        if (value == null) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }
}
