package com.hospitalflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request/response records for the inventory module. Quantities are Long on the wire so values
 * beyond int range are rejected by @Max with a 400 instead of failing JSON parsing.
 */
public final class InventoryDtos {

    private InventoryDtos() {}

    public record MedicineDto(
            Long id, String name, String strength, String dosageForm, String unit,
            int quantity, int reorderThreshold, String status, LocalDateTime updatedAt) {

        static MedicineDto of(Medicine m) {
            return new MedicineDto(m.getId(), m.getName(), m.getStrength(), m.getDosageForm(), m.getUnit(),
                    m.getQuantity(), m.getReorderThreshold(), m.getStockStatus().name(), m.getUpdatedAt());
        }
    }

    public record InventorySummaryDto(int totalMedicines, int inStock, int lowStock, int outOfStock) {}

    public record StockMovementDto(
            Long id, Long medicineId, String medicineLabel, String unit, String type,
            int quantityChange, int quantityAfter, Long prescriptionId,
            String performedBy, LocalDateTime createdAt) {

        static StockMovementDto of(StockMovement s) {
            Medicine m = s.getMedicine();
            return new StockMovementDto(s.getId(), m.getId(), m.label(), m.getUnit(), s.getType().name(),
                    s.getQuantityChange(), s.getQuantityAfter(), s.getPrescriptionId(),
                    s.getPerformedBy(), s.getCreatedAt());
        }
    }

    /** Inventory medicines whose name/strength appear in a pending prescription's text. */
    public record PrescriptionMatchesDto(Long prescriptionId, List<MedicineDto> matches) {}

    public record CreateMedicineRequest(
            @NotBlank(message = "Medicine name is required")
            @Size(max = 120, message = "Medicine name must be at most 120 characters")
            String name,
            @Size(max = 60, message = "Strength must be at most 60 characters")
            String strength,
            @Size(max = 60, message = "Dosage form must be at most 60 characters")
            String dosageForm,
            @NotBlank(message = "Unit of measurement is required")
            @Size(max = 30, message = "Unit must be at most 30 characters")
            String unit,
            @NotNull(message = "Initial quantity is required")
            @Min(value = 0, message = "Initial quantity cannot be negative")
            @Max(value = InventoryService.MAX_STOCK, message = "Initial quantity cannot exceed " + InventoryService.MAX_STOCK)
            Long initialQuantity,
            @NotNull(message = "Reorder threshold is required")
            @Min(value = 0, message = "Reorder threshold cannot be negative")
            @Max(value = InventoryService.MAX_STOCK, message = "Reorder threshold cannot exceed " + InventoryService.MAX_STOCK)
            Long reorderThreshold) {}

    public record RestockRequest(
            @NotNull(message = "Restock quantity is required")
            @Min(value = 1, message = "Restock quantity must be at least 1")
            @Max(value = InventoryService.MAX_STOCK, message = "Restock quantity cannot exceed " + InventoryService.MAX_STOCK)
            Long quantity) {}

    /** Body of POST /api/labpharmacy/prescriptions/{id}/dispense. */
    public record DispenseRequest(@Valid List<DispenseItem> items) {}

    /** One inventory medicine to hand over, with the quantity in that medicine's unit. */
    public record DispenseItem(
            @NotNull(message = "Medicine is required")
            Long medicineId,
            @NotNull(message = "Dispense quantity is required")
            @Min(value = 1, message = "Dispense quantity must be at least 1")
            @Max(value = InventoryService.MAX_STOCK, message = "Dispense quantity cannot exceed " + InventoryService.MAX_STOCK)
            Long quantity) {}
}
