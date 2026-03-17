package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.PujaSamagriItemRequest;
import com.astro.backend.RequestDTO.PujaSamagriMasterRequest;
import com.astro.backend.Services.PujaService;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/web/puja-samagri")
@RequiredArgsConstructor
public class AdminPujaSamagriController {

    private final PujaService pujaService;

    @GetMapping("/master")
    public ResponseEntity<ApiResponse<Object>> listMaster() {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.getAllSamagriMasterForAdmin(),
                            true,
                            "Samagri masters fetched successfully"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "SAMAGRI_MASTER_LIST_FAILED"
                    )
            );
        }
    }

    @PostMapping("/master")
    public ResponseEntity<ApiResponse<Object>> createMaster(@RequestBody PujaSamagriMasterRequest request) {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.createSamagriMaster(request),
                            true,
                            "Samagri master created"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "SAMAGRI_MASTER_CREATE_FAILED"
                    )
            );
        }
    }

    @PutMapping("/master/{id}")
    public ResponseEntity<ApiResponse<Object>> updateMaster(@PathVariable Long id, @RequestBody PujaSamagriMasterRequest request) {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.updateSamagriMaster(id, request),
                            true,
                            "Samagri master updated"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "SAMAGRI_MASTER_UPDATE_FAILED"
                    )
            );
        }
    }

    @DeleteMapping("/master/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteMaster(@PathVariable Long id) {
        try {
            pujaService.softDeleteSamagriMaster(id);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            id,
                            true,
                            "Samagri master deleted"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "SAMAGRI_MASTER_DELETE_FAILED"
                    )
            );
        }
    }

    @GetMapping("/puja/{pujaId}")
    public ResponseEntity<ApiResponse<Object>> listPujaItems(@PathVariable Long pujaId) {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.getPujaSamagriForAdmin(pujaId),
                            true,
                            "Puja samagri fetched successfully"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "PUJA_SAMAGRI_LIST_FAILED"
                    )
            );
        }
    }

    @PostMapping("/puja/{pujaId}")
    public ResponseEntity<ApiResponse<Object>> addPujaItem(@PathVariable Long pujaId, @RequestBody PujaSamagriItemRequest request) {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.addSamagriToPuja(
                                    pujaId,
                                    request.getSamagriMasterId(),
                                    request.getQuantity(),
                                    request.getUnit(),
                                    request.getNotes(),
                                    request.getDisplayOrder()
                            ),
                            true,
                            "Puja samagri saved"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "PUJA_SAMAGRI_ADD_FAILED"
                    )
            );
        }
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<Object>> updatePujaItem(@PathVariable Long itemId, @RequestBody PujaSamagriItemRequest request) {
        try {
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            pujaService.updatePujaSamagriItem(
                                    itemId,
                                    request.getQuantity(),
                                    request.getUnit(),
                                    request.getNotes(),
                                    request.getDisplayOrder(),
                                    request.getIsActive()
                            ),
                            true,
                            "Puja samagri item updated"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "PUJA_SAMAGRI_UPDATE_FAILED"
                    )
            );
        }
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<Object>> deletePujaItem(@PathVariable Long itemId) {
        try {
            pujaService.deletePujaSamagriItem(itemId);
            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(
                            itemId,
                            true,
                            "Puja samagri item deleted"
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseUtils.createFailureResponse(
                            e.getMessage(),
                            false,
                            "PUJA_SAMAGRI_DELETE_FAILED"
                    )
            );
        }
    }
}
