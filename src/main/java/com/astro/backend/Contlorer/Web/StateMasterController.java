package com.astro.backend.Contlorer.Web;

import com.astro.backend.Entity.StateMaster;
import com.astro.backend.Repositry.StateMasterRepository;
import com.astro.backend.RequestDTO.StateMasterRequest;
import com.astro.backend.ResponseDTO.StateMasterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/master/state")
@RequiredArgsConstructor
public class StateMasterController {

    private final StateMasterRepository stateRepository;

    /**
     * Get all states
     */
    @GetMapping("/all")
    public ResponseEntity<List<StateMaster>> getAllStates() {
        try {
            List<StateMaster> states = stateRepository.findAll();
            return ResponseEntity.ok(states);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active states only
     */
    @GetMapping("/active")
    public ResponseEntity<List<StateMaster>> getActiveStates() {
        try {
            List<StateMaster> states = stateRepository.findByIsActiveOrderByName(true);
            return ResponseEntity.ok(states);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get state by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StateMaster> getStateById(@PathVariable Long id) {
        try {
            StateMaster state = stateRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("State not found"));
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Insert new state
     */
    @PostMapping("/insert")
    public ResponseEntity<StateMasterResponse> insertState(@Valid @RequestBody StateMasterRequest request) {
        try {
            StateMaster state = StateMaster.builder()
                    .name(request.getName())
                    .code(request.getCode())
                    .country(request.getCountry())
                    .description(request.getDescription())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            StateMaster savedState = stateRepository.save(state);

            return ResponseEntity.status(HttpStatus.CREATED).body(StateMasterResponse.builder()
                    .id(savedState.getId())
                    .name(savedState.getName())
                    .code(savedState.getCode())
                    .country(savedState.getCountry())
                    .description(savedState.getDescription())
                    .isActive(savedState.getIsActive())
                    .createdAt(savedState.getCreatedAt())
                    .status(true)
                    .message("State inserted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StateMasterResponse.builder()
                            .status(false)
                            .message("Failed to insert state: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update state
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<StateMasterResponse> updateState(@PathVariable Long id, 
            @Valid @RequestBody StateMasterRequest request) {
        try {
            StateMaster state = stateRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("State not found"));

            state.setName(request.getName());
            state.setCode(request.getCode());
            state.setCountry(request.getCountry());
            state.setDescription(request.getDescription());
            if (request.getIsActive() != null) {
                state.setIsActive(request.getIsActive());
            }

            StateMaster updatedState = stateRepository.save(state);

            return ResponseEntity.ok(StateMasterResponse.builder()
                    .id(updatedState.getId())
                    .name(updatedState.getName())
                    .code(updatedState.getCode())
                    .country(updatedState.getCountry())
                    .description(updatedState.getDescription())
                    .isActive(updatedState.getIsActive())
                    .updatedAt(updatedState.getUpdatedAt())
                    .status(true)
                    .message("State updated successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StateMasterResponse.builder()
                            .status(false)
                            .message("Failed to update state: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Delete state
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<StateMasterResponse> deleteState(@PathVariable Long id) {
        try {
            StateMaster state = stateRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("State not found"));

            stateRepository.deleteById(id);

            return ResponseEntity.ok(StateMasterResponse.builder()
                    .id(state.getId())
                    .name(state.getName())
                    .status(true)
                    .message("State deleted successfully")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StateMasterResponse.builder()
                            .status(false)
                            .message("Failed to delete state: " + e.getMessage())
                            .build());
        }
    }
}
