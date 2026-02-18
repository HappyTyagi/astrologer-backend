package com.astro.backend.Contlorer.Web;

import com.astro.backend.RequestDTO.PujaSamagriItemRequest;
import com.astro.backend.RequestDTO.PujaSamagriMasterRequest;
import com.astro.backend.Services.PujaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/web/puja-samagri")
@RequiredArgsConstructor
public class AdminPujaSamagriController {

    private final PujaService pujaService;

    @GetMapping("/master")
    public ResponseEntity<?> listMaster() {
        return ResponseEntity.ok(Map.of(
                "status", true,
                "items", pujaService.getAllSamagriMasterForAdmin()
        ));
    }

    @PostMapping("/master")
    public ResponseEntity<?> createMaster(@RequestBody PujaSamagriMasterRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Samagri master created",
                "item", pujaService.createSamagriMaster(request.getName(), request.getDescription(), request.getIsActive())
        ));
    }

    @PutMapping("/master/{id}")
    public ResponseEntity<?> updateMaster(@PathVariable Long id, @RequestBody PujaSamagriMasterRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Samagri master updated",
                "item", pujaService.updateSamagriMaster(id, request.getName(), request.getDescription(), request.getIsActive())
        ));
    }

    @DeleteMapping("/master/{id}")
    public ResponseEntity<?> deleteMaster(@PathVariable Long id) {
        pujaService.softDeleteSamagriMaster(id);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Samagri master deleted",
                "id", id
        ));
    }

    @GetMapping("/puja/{pujaId}")
    public ResponseEntity<?> listPujaItems(@PathVariable Long pujaId) {
        return ResponseEntity.ok(Map.of(
                "status", true,
                "pujaId", pujaId,
                "items", pujaService.getPujaSamagriForAdmin(pujaId)
        ));
    }

    @PostMapping("/puja/{pujaId}")
    public ResponseEntity<?> addPujaItem(@PathVariable Long pujaId, @RequestBody PujaSamagriItemRequest request) {
        return ResponseEntity.ok(pujaService.addSamagriToPuja(
                pujaId,
                request.getSamagriMasterId(),
                request.getQuantity(),
                request.getUnit(),
                request.getNotes(),
                request.getDisplayOrder()
        ));
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<?> updatePujaItem(@PathVariable Long itemId, @RequestBody PujaSamagriItemRequest request) {
        return ResponseEntity.ok(pujaService.updatePujaSamagriItem(
                itemId,
                request.getQuantity(),
                request.getUnit(),
                request.getNotes(),
                request.getDisplayOrder(),
                request.getIsActive()
        ));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<?> deletePujaItem(@PathVariable Long itemId) {
        pujaService.deletePujaSamagriItem(itemId);
        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", "Puja samagri item deleted",
                "itemId", itemId
        ));
    }
}
