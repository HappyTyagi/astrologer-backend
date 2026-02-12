package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.RequestDTO.LinkedProfileCreateRequest;
import com.astro.backend.Services.LinkedMobileProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile/linked")
@RequiredArgsConstructor
public class LinkedMobileProfileController {

    private final LinkedMobileProfileService linkedMobileProfileService;

    @GetMapping("/{mobileNo}")
    public ResponseEntity<?> list(@PathVariable String mobileNo) {
        return ResponseEntity.ok(linkedMobileProfileService.listByMobile(mobileNo));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody LinkedProfileCreateRequest request) {
        try {
            return ResponseEntity.ok(linkedMobileProfileService.create(request));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{profileId}/make-primary")
    public ResponseEntity<?> makePrimary(
            @PathVariable Long profileId,
            @RequestParam("mobileNo") String mobileNo
    ) {
        try {
            return ResponseEntity.ok(linkedMobileProfileService.makePrimary(mobileNo, profileId));
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

