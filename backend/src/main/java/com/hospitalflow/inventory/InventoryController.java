package com.hospitalflow.inventory;

import com.hospitalflow.inventory.InventoryDtos.CreateMedicineRequest;
import com.hospitalflow.inventory.InventoryDtos.InventorySummaryDto;
import com.hospitalflow.inventory.InventoryDtos.MedicineDto;
import com.hospitalflow.inventory.InventoryDtos.PrescriptionMatchesDto;
import com.hospitalflow.inventory.InventoryDtos.RestockRequest;
import com.hospitalflow.inventory.InventoryDtos.StockMovementDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Medicine inventory for the Pharmacist dashboard. Dispensing itself stays on the existing
 * POST /api/labpharmacy/prescriptions/{id}/dispense endpoint, which now deducts stock.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** All medicines with derived stock status */
    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<MedicineDto> listMedicines() {
        return inventoryService.listMedicines();
    }

    /** Counts per stock status */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public InventorySummaryDto summary() {
        return inventoryService.summary();
    }

    /** One medicine with its current stock status */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public MedicineDto getMedicine(@PathVariable Long id) {
        return inventoryService.getMedicine(id);
    }

    /** Pharmacist adds a new medicine */
    @PostMapping
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<MedicineDto> addMedicine(@Valid @RequestBody CreateMedicineRequest body, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addMedicine(body, auth.getName()));
    }

    /** Pharmacist increases the stock of an existing medicine */
    @PostMapping("/{id}/restock")
    @PreAuthorize("hasRole('PHARMACIST')")
    public MedicineDto restock(@PathVariable Long id, @Valid @RequestBody RestockRequest body, Authentication auth) {
        return inventoryService.restock(id, body.quantity(), auth.getName());
    }

    /** Latest 50 stock movements (audit trail) */
    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<StockMovementDto> recentMovements() {
        return inventoryService.recentMovements();
    }

    /** Matching inventory medicines for each pending prescription */
    @GetMapping("/prescriptions/pending/matches")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<PrescriptionMatchesDto> pendingPrescriptionMatches() {
        return inventoryService.pendingPrescriptionMatches();
    }
}
