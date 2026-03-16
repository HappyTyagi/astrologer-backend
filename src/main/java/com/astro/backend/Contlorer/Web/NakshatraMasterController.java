package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.NakshatraMaster;
import com.astro.backend.Repositry.NakshatraMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/master/nakshatra")
@RequiredArgsConstructor
public class NakshatraMasterController {

    private final NakshatraMasterRepository nakshatraMasterRepository;

    @GetMapping("/all")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(nakshatraMasterRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        return ResponseEntity.ok(nakshatraMasterRepository.findByIsActiveOrderByName(true));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name") == null ? "" : payload.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Nakshatra name is required");
        }
        String hiName = payload.get("hiName") == null
                ? (payload.get("hi_name") == null ? null : payload.get("hi_name").toString().trim())
                : payload.get("hiName").toString().trim();
        String slug = payload.get("slug") == null ? null : payload.get("slug").toString().trim();
        String image = parseImage(payload);
        String description = payload.get("description") == null ? null : payload.get("description").toString().trim();
        String descriptionHi = payload.get("descriptionHi") == null
                ? (payload.get("description_hi") == null ? null : payload.get("description_hi").toString().trim())
                : payload.get("descriptionHi").toString().trim();
        String sourceUrl = payload.get("sourceUrl") == null
                ? (payload.get("source_url") == null ? null : payload.get("source_url").toString().trim())
                : payload.get("sourceUrl").toString().trim();
        NakshatraMaster saved = nakshatraMasterRepository.save(
                NakshatraMaster.builder()
                        .name(name)
                        .slug(slug == null || slug.isBlank() ? null : slug)
                        .hiName(hiName == null || hiName.isBlank() ? null : hiName)
                        .image(image)
                        .description(description)
                        .descriptionHi(descriptionHi == null || descriptionHi.isBlank() ? null : descriptionHi)
                        .sourceUrl(sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl)
                        .isActive(true)
                        .build()
        );
        return ResponseEntity.ok(Map.of("status", true, "message", "Nakshatra inserted successfully", "item", saved));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        NakshatraMaster existing = nakshatraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nakshatra not found: " + id));
        if (payload.containsKey("name") && payload.get("name") != null) {
            String name = payload.get("name").toString().trim();
            if (name.isEmpty()) {
                throw new RuntimeException("Nakshatra name is required");
            }
            existing.setName(name);
        }
        if (payload.containsKey("description")) {
            existing.setDescription(payload.get("description") == null ? null : payload.get("description").toString().trim());
        }
        if (payload.containsKey("descriptionHi") || payload.containsKey("description_hi")) {
            Object raw = payload.containsKey("descriptionHi") ? payload.get("descriptionHi") : payload.get("description_hi");
            String descriptionHi = raw == null ? null : raw.toString().trim();
            existing.setDescriptionHi((descriptionHi == null || descriptionHi.isBlank()) ? null : descriptionHi);
        }
        if (payload.containsKey("hiName") || payload.containsKey("hi_name")) {
            Object raw = payload.containsKey("hiName") ? payload.get("hiName") : payload.get("hi_name");
            String hiName = raw == null ? null : raw.toString().trim();
            existing.setHiName((hiName == null || hiName.isBlank()) ? null : hiName);
        }
        if (payload.containsKey("slug")) {
            String slug = payload.get("slug") == null ? null : payload.get("slug").toString().trim();
            existing.setSlug((slug == null || slug.isBlank()) ? null : slug);
        }
        if (payload.containsKey("image") || payload.containsKey("imageBase64")) {
            existing.setImage(parseImage(payload));
        }
        if (payload.containsKey("sourceUrl") || payload.containsKey("source_url")) {
            Object raw = payload.containsKey("sourceUrl") ? payload.get("sourceUrl") : payload.get("source_url");
            String sourceUrl = raw == null ? null : raw.toString().trim();
            existing.setSourceUrl((sourceUrl == null || sourceUrl.isBlank()) ? null : sourceUrl);
        }
        if (payload.containsKey("isActive") && payload.get("isActive") instanceof Boolean flag) {
            existing.setIsActive(flag);
        }
        NakshatraMaster saved = nakshatraMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of("status", true, "message", "Nakshatra updated successfully", "item", saved));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        NakshatraMaster existing = nakshatraMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nakshatra not found: " + id));
        existing.setIsActive(false);
        nakshatraMasterRepository.save(existing);
        return ResponseEntity.ok(Map.of("status", true, "message", "Nakshatra deleted successfully", "id", id));
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
