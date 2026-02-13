package com.astro.backend.Contlorer.Mobile;

import com.astro.backend.Entity.CallRecord;
import com.astro.backend.RequestDTO.CallRecordRequest;
import com.astro.backend.ResponseDTO.CallRecordResponse;
import com.astro.backend.Services.CallRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/call")
@RequiredArgsConstructor
public class CallRecordController {

    private final CallRecordService callRecordService;

    @PostMapping("/record")
    public ResponseEntity<CallRecordResponse> saveCallRecord(@Valid @RequestBody CallRecordRequest request) {
        try {
            CallRecord saved = callRecordService.saveRecord(request);
            return ResponseEntity.ok(
                    CallRecordResponse.builder()
                            .success(true)
                            .message("Call record saved")
                            .id(saved.getId())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CallRecordResponse.builder()
                            .success(false)
                            .message("Failed to save call record: " + e.getMessage())
                            .build());
        }
    }
}
