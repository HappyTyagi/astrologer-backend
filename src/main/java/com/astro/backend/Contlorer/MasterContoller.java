package com.astro.backend.Contlorer;

import com.astro.backend.ResponseDTO.MasterResponse;
import com.astro.backend.Services.MasterService;
import com.astro.backend.apiResponse.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/abdm/master")
@CrossOrigin(origins = "*")
@Tag(name = "ABDM Master Data", description = "APIs to fetch master/reference data required for ABHA verification workflows")
public class MasterContoller {

    @Autowired
    private MasterService masterService;

    @Operation(summary = "Fetch ABHA Verification Types", description = "This API returns the list of available verification types such as ABHA number, Mobile number, or other supported identifiers.")
    @PostMapping("/get-list")
    public ResponseEntity<ApiResponse<List<MasterResponse>>> getVerificationTypeList() {
        return ResponseEntity.ok(masterService.getVerificationType());
    }
}
