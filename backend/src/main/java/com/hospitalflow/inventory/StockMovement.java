package com.hospitalflow.inventory;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Audit row for every stock change. The (prescription_id, medicine_id) unique constraint is a
 * database-level guard against deducting the same medicine twice for one prescription.
 */
@Entity
@Table(name = "stock_movements",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_movements_prescription_medicine",
                columnNames = {"prescription_id", "medicine_id"}))
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** Signed change: positive for INITIAL/RESTOCK, negative for DISPENSE. */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Type { INITIAL, RESTOCK, DISPENSE }

    public StockMovement() {}

    public StockMovement(Medicine medicine, Type type, int quantityChange, Long prescriptionId,
                         String performedBy, LocalDateTime createdAt) {
        this.medicine = medicine;
        this.type = type;
        this.quantityChange = quantityChange;
        this.quantityAfter = medicine.getQuantity();
        this.prescriptionId = prescriptionId;
        this.performedBy = performedBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Medicine getMedicine() { return medicine; }
    public Type getType() { return type; }
    public int getQuantityChange() { return quantityChange; }
    public int getQuantityAfter() { return quantityAfter; }
    public Long getPrescriptionId() { return prescriptionId; }
    public String getPerformedBy() { return performedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
