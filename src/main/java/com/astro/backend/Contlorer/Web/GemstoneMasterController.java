package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.GemstoneMaster;
import com.astro.backend.Repositry.GemstoneMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/master/gemstone")
@RequiredArgsConstructor
public class GemstoneMasterController {

    private final GemstoneMasterRepository gemstoneMasterRepository;

    @GetMapping("/all")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(gemstoneMasterRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        return ResponseEntity.ok(gemstoneMasterRepository.findByIsActiveOrderByName(true));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") == null ? "" : payload.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Gemstone name is required");
        }
        String image = parseImage(payload);
        String associatedPlanet = payload.get("associatedPlanet") == null ? null : payload.get("associatedPlanet").toString().trim();
        String description = payload.get("description") == null ? null : payload.get("description").toString().trim();
        GemstoneMaster saved = gemstoneMasterRepository.save(
                GemstoneMaster.builder()
                        .name(name)
                        .image(image)
                        .associatedPlanet(associatedPlanet)
                        .description(description)
                        .isActive(true)
                        .build()
        );
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gemstone inserted successfully",
                "item", saved
        ));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        GemstoneMaster existing = gemstoneMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gemstone not found: " + id));
        if (payload.containsKey("name") && payload.get("name") != null) {
            String name = payload.get("name").toString().trim();
            if (!name.isEmpty()) {
                existing.setName(name);
            }
        }
        if (payload.containsKey("associatedPlanet")) {
            existing.setAssociatedPlanet(payload.get("associatedPlanet") == null ? null : payload.get("associatedPlanet").toString().trim());
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
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gemstone updated successfully",
                "item", gemstoneMasterRepository.save(existing)
        ));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        GemstoneMaster existing = gemstoneMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gemstone not found: " + id));
        existing.setIsActive(false);
        gemstoneMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Gemstone deleted successfully",
                "id", id
        ));
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
