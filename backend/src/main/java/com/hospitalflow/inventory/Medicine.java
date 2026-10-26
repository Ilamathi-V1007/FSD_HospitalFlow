package com.hospitalflow.inventory;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

/**
 * A stocked medicine. Identity is name + strength + dosage form, normalised into
 * {@code medicine_key} so "Paracetamol 500 mg" and "paracetamol 500mg" are one record.
 * Stock status is never stored; it is derived from quantity and reorder threshold.
 */
@Entity
@Table(name = "medicines",
        uniqueConstraints = @UniqueConstraint(name = "uk_medicines_medicine_key", columnNames = "medicine_key"))
@Check(constraints = "quantity >= 0 AND reorder_threshold >= 0")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String strength;

    @Column(name = "dosage_form", length = 60)
    private String dosageForm;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Column(name = "medicine_key", nullable = false, length = 300)
    private String medicineKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Medicine() {}

    public StockStatus getStockStatus() {
        return StockStatus.of(quantity, reorderThreshold);
    }

    /** Human-readable label, e.g. "Amoxicillin 500mg (Capsule)". */
    public String label() {
        StringBuilder sb = new StringBuilder(name);
        if (strength != null) sb.append(' ').append(strength);
        if (dosageForm != null) sb.append(" (").append(dosageForm).append(')');
        return sb.toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    public String getMedicineKey() { return medicineKey; }
    public void setMedicineKey(String medicineKey) { this.medicineKey = medicineKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
