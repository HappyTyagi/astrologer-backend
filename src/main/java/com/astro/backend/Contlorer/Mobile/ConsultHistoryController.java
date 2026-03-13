package com.astro.backend.Contlorer.Mobile;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/consult")
public class ConsultHistoryController {

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getConsultHistory(@PathVariable Long userId) {
        // TODO: Fetch audio/video call history from DB
        // Sample response
        List<Map<String, Object>> history = List.of(
            Map.of("type", "audio", "date", "2026-02-24", "duration", "15 min", "with", "Astrologer A"),
            Map.of("type", "video", "date", "2026-02-20", "duration", "30 min", "with", "Astrologer B")
        );
        return ResponseEntity.ok(history);
    }
}
