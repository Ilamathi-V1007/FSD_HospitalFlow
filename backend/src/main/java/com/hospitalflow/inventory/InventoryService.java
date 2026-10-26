package com.hospitalflow.inventory;

import com.hospitalflow.inventory.InventoryDtos.CreateMedicineRequest;
import com.hospitalflow.inventory.InventoryDtos.DispenseItem;
import com.hospitalflow.inventory.InventoryDtos.DispenseRequest;
import com.hospitalflow.inventory.InventoryDtos.InventorySummaryDto;
import com.hospitalflow.inventory.InventoryDtos.MedicineDto;
import com.hospitalflow.inventory.InventoryDtos.PrescriptionMatchesDto;
import com.hospitalflow.inventory.InventoryDtos.StockMovementDto;
import com.hospitalflow.labpharmacy.Prescription;
import com.hospitalflow.labpharmacy.PrescriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class InventoryService {

    /** Upper bound for any stock level or single movement; keeps all arithmetic far from int overflow. */
    public static final int MAX_STOCK = 1_000_000;

    private final MedicineRepository medicineRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PrescriptionRepository prescriptionRepository;

    public InventoryService(
            MedicineRepository medicineRepository,
            StockMovementRepository stockMovementRepository,
            PrescriptionRepository prescriptionRepository) {
        this.medicineRepository = medicineRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    // ─── Queries ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MedicineDto> listMedicines() {
        return medicineRepository.findAllByOrderByNameAscStrengthAsc().stream().map(MedicineDto::of).toList();
    }

    @Transactional(readOnly = true)
    public MedicineDto getMedicine(Long id) {
        return MedicineDto.of(medicineRepository.findById(id)
                .orElseThrow(() -> InventoryException.notFound("Medicine not found: " + id)));
    }

    @Transactional(readOnly = true)
    public InventorySummaryDto summary() {
        List<Medicine> all = medicineRepository.findAll();
        int in = 0, low = 0, out = 0;
        for (Medicine m : all) {
            switch (m.getStockStatus()) {
                case IN_STOCK -> in++;
                case LOW_STOCK -> low++;
                case OUT_OF_STOCK -> out++;
            }
        }
        return new InventorySummaryDto(all.size(), in, low, out);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> recentMovements() {
        return stockMovementRepository.findTop50ByOrderByCreatedAtDescIdDesc().stream()
                .map(StockMovementDto::of).toList();
    }

    /** For each pending prescription, the inventory medicines its text actually names. */
    @Transactional(readOnly = true)
    public List<PrescriptionMatchesDto> pendingPrescriptionMatches() {
        List<Medicine> medicines = medicineRepository.findAllByOrderByNameAscStrengthAsc();
        return prescriptionRepository.findByStatus(Prescription.Status.PENDING).stream()
                .map(rx -> new PrescriptionMatchesDto(rx.getId(), medicines.stream()
                        .filter(m -> MedicineMatcher.matches(rx.getMedicineDetails(), m))
                        .map(MedicineDto::of).toList()))
                .toList();
    }

    // ─── Stock changes ───────────────────────────────────────────────────────────

    @Transactional
    public MedicineDto addMedicine(CreateMedicineRequest req, String performedBy) {
        String name = clean(req.name());
        String unit = clean(req.unit());
        if (name == null) throw InventoryException.badRequest("Medicine name is required");
        if (unit == null) throw InventoryException.badRequest("Unit of measurement is required");
        int quantity = checkedQuantity(req.initialQuantity(), 0, "Initial quantity");
        int threshold = checkedQuantity(req.reorderThreshold(), 0, "Reorder threshold");

        Medicine m = new Medicine();
        m.setName(name);
        m.setStrength(clean(req.strength()));
        m.setDosageForm(clean(req.dosageForm()));
        m.setUnit(unit);
        m.setQuantity(quantity);
        m.setReorderThreshold(threshold);
        m.setMedicineKey(MedicineMatcher.key(m.getName(), m.getStrength(), m.getDosageForm()));

        if (medicineRepository.existsByMedicineKey(m.getMedicineKey())) {
            throw InventoryException.conflict(m.label() + " already exists in inventory. Restock it instead.");
        }
        try {
            // flush now so a concurrent duplicate insert surfaces here as a 409, not at commit
            medicineRepository.saveAndFlush(m);
        } catch (DataIntegrityViolationException e) {
            throw InventoryException.conflict(m.label() + " already exists in inventory. Restock it instead.");
        }
        stockMovementRepository.save(new StockMovement(
                m, StockMovement.Type.INITIAL, quantity, null, performedBy, m.getCreatedAt()));
        return MedicineDto.of(m);
    }

    @Transactional
    public MedicineDto restock(Long medicineId, Long quantity, String performedBy) {
        int amount = checkedQuantity(quantity, 1, "Restock quantity");
        Medicine m = medicineRepository.findByIdForUpdate(medicineId)
                .orElseThrow(() -> InventoryException.notFound("Medicine not found: " + medicineId));

        long newQuantity = (long) m.getQuantity() + amount;
        if (newQuantity > MAX_STOCK) {
            throw InventoryException.badRequest("Restocking " + m.label() + " by " + amount
                    + " would exceed the maximum stock level of " + MAX_STOCK + ".");
        }
        LocalDateTime now = LocalDateTime.now();
        m.setQuantity((int) newQuantity);
        m.setUpdatedAt(now);
        stockMovementRepository.save(new StockMovement(
                m, StockMovement.Type.RESTOCK, amount, null, performedBy, now));
        return MedicineDto.of(m);
    }

    /**
     * Dispense a pending prescription, deducting each selected medicine's stock.
     *
     * <p>Everything happens in one transaction: the prescription row is locked first (so a second
     * dispense of the same prescription waits, then sees DISPENSED), then each medicine row is locked
     * in ascending id order (so concurrent dispenses can't deadlock or oversell). All checks run before
     * any change; any failure rolls back both the stock deduction and the status change.
     */
    @Transactional
    public Prescription dispense(Long prescriptionId, DispenseRequest req, String performedBy) {
        Prescription rx = prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> InventoryException.notFound("Prescription not found"));
        if (rx.getStatus() == Prescription.Status.DISPENSED) {
            throw InventoryException.conflict("Prescription is already dispensed");
        }
        if (stockMovementRepository.existsByPrescriptionId(rx.getId())) {
            throw InventoryException.conflict("Stock has already been deducted for this prescription");
        }

        List<DispenseItem> items = req == null || req.items() == null ? List.of() : req.items();
        if (items.isEmpty()) {
            throw InventoryException.badRequest(
                    "Select the inventory medicine(s) and quantity to dispense for this prescription.");
        }
        Map<Long, Integer> quantityByMedicine = new TreeMap<>(); // sorted => consistent lock order
        for (DispenseItem item : items) {
            if (item == null || item.medicineId() == null) {
                throw InventoryException.badRequest("Each dispensed item must reference an inventory medicine.");
            }
            int qty = checkedQuantity(item.quantity(), 1, "Dispense quantity");
            if (quantityByMedicine.put(item.medicineId(), qty) != null) {
                throw InventoryException.badRequest("Each medicine may appear only once per dispense.");
            }
        }

        List<Medicine> locked = new ArrayList<>();
        for (Long medicineId : quantityByMedicine.keySet()) {
            Medicine m = medicineRepository.findByIdForUpdate(medicineId)
                    .orElseThrow(() -> InventoryException.badRequest("Medicine #" + medicineId + " is not in inventory."));
            if (!MedicineMatcher.matches(rx.getMedicineDetails(), m)) {
                throw InventoryException.badRequest(m.label() + " does not match this prescription (\""
                        + rx.getMedicineDetails() + "\"). No stock was deducted.");
            }
            locked.add(m);
        }

        for (Medicine m : locked) {
            int required = quantityByMedicine.get(m.getId());
            if (m.getQuantity() == 0) {
                throw InventoryException.conflict(m.label() + " is out of stock. No stock was deducted.");
            }
            if (m.getQuantity() < required) {
                throw InventoryException.conflict("Insufficient stock for " + m.label() + ": "
                        + m.getQuantity() + " " + m.getUnit() + " available, " + required
                        + " required. No stock was deducted.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Medicine m : locked) {
            int required = quantityByMedicine.get(m.getId());
            m.setQuantity(m.getQuantity() - required);
            m.setUpdatedAt(now);
            stockMovementRepository.save(new StockMovement(
                    m, StockMovement.Type.DISPENSE, -required, rx.getId(), performedBy, now));
        }

        rx.setStatus(Prescription.Status.DISPENSED);
        rx.setDispensedAt(now);
        rx.setDispensedBy(performedBy);
        try {
            return prescriptionRepository.saveAndFlush(rx);
        } catch (DataIntegrityViolationException e) {
            throw InventoryException.conflict("Dispensing conflicted with another update. No changes were saved.");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Server-side range check (duplicates bean validation for callers that bypass the controller). */
    private static int checkedQuantity(Long value, int min, String field) {
        if (value == null) throw InventoryException.badRequest(field + " is required");
        if (value < min) {
            throw InventoryException.badRequest(field + (min == 0 ? " cannot be negative" : " must be at least " + min));
        }
        if (value > MAX_STOCK) throw InventoryException.badRequest(field + " cannot exceed " + MAX_STOCK);
        return value.intValue();
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }
}
