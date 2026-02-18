package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.Remides;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.RequestDTO.CreateRemidesRequest;
import com.astro.backend.RequestDTO.UpdateRemidesRequest;
import com.astro.backend.ResponseDTO.RemidesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/remides")
@RequiredArgsConstructor
public class RemidesController {

    private final RemidesRepository remidesRepository;

    @PostMapping
    public ResponseEntity<RemidesResponse> createRemides(@Valid @RequestBody CreateRemidesRequest request) {
        try {
            Remides remides = Remides.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .tokenAmount(request.getTokenAmount())
                    .discountPercentage(request.getDiscountPercentage())
                    .currency(request.getCurrency())
                    .imageBase64(request.getImageBase64())
                    .build();

                Remides saved = remidesRepository.save(Objects.requireNonNull(remides, "remides"));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(saved, true, "Remides created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RemidesResponse.builder()
                            .status(false)
                            .message("Failed to create remides: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RemidesResponse> getRemidesById(@PathVariable Long id) {
        Long remidesId = Objects.requireNonNull(id, "id");
        return remidesRepository.findById(remidesId)
                .map(remides -> ResponseEntity.ok(toResponse(remides, true, "Remides fetched successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RemidesResponse.builder()
                    .status(false)
                    .message("Remides not found with ID: " + remidesId)
                                .build()));
    }

    @GetMapping
    public ResponseEntity<List<RemidesResponse>> getRemides(@RequestParam(value = "userId", required = false) Long userId) {
        List<Remides> remidesList = (userId == null)
                ? remidesRepository.findAll()
                : remidesRepository.findByUserId(userId);

        List<RemidesResponse> response = remidesList.stream()
                .map(remides -> toResponse(remides, true, "Remides fetched successfully"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RemidesResponse> updateRemides(@PathVariable Long id, @RequestBody UpdateRemidesRequest request) {
        try {
            Long remidesId = Objects.requireNonNull(id, "id");
            Remides remides = remidesRepository.findById(remidesId)
                    .orElseThrow(() -> new RuntimeException("Remides not found with ID: " + remidesId));

            if (request.getUserId() != null) {
                remides.setUserId(request.getUserId());
            }
            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                remides.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                remides.setDescription(request.getDescription());
            }
            if (request.getPrice() != null) {
                remides.setPrice(request.getPrice());
            }
            if (request.getTokenAmount() != null) {
                remides.setTokenAmount(request.getTokenAmount());
            }
            if (request.getDiscountPercentage() != null) {
                remides.setDiscountPercentage(request.getDiscountPercentage());
            }
            if (request.getCurrency() != null && !request.getCurrency().isEmpty()) {
                remides.setCurrency(request.getCurrency());
            }
            if (request.getImageBase64() != null) {
                remides.setImageBase64(request.getImageBase64());
            }

            Remides updated = remidesRepository.save(Objects.requireNonNull(remides, "remides"));
            return ResponseEntity.ok(toResponse(updated, true, "Remides updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(RemidesResponse.builder()
                            .status(false)
                            .message("Failed to update remides: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RemidesResponse> deleteRemides(@PathVariable Long id) {
        try {
            Long remidesId = Objects.requireNonNull(id, "id");
            if (!remidesRepository.existsById(remidesId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RemidesResponse.builder()
                                .status(false)
                    .message("Remides not found with ID: " + remidesId)
                                .build());
            }
            remidesRepository.deleteById(remidesId);
            return ResponseEntity.ok(RemidesResponse.builder()
                    .status(true)
                    .message("Remides deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RemidesResponse.builder()
                            .status(false)
                            .message("Failed to delete remides: " + e.getMessage())
                            .build());
        }
    }

    private RemidesResponse toResponse(Remides remides, boolean status, String message) {
        return RemidesResponse.builder()
                .id(remides.getId())
                .userId(remides.getUserId())
                .title(remides.getTitle())
                .description(remides.getDescription())
                .price(remides.getPrice())
                .tokenAmount(remides.getTokenAmount())
                .discountPercentage(remides.getDiscountPercentage())
                .finalPrice(remides.getFinalPrice())
                .currency(remides.getCurrency())
                .imageBase64(remides.getImageBase64())
                .createdAt(remides.getCreatedAt())
                .updatedAt(remides.getUpdatedAt())
                .status(status)
                .message(message)
                .build();
    }
}
