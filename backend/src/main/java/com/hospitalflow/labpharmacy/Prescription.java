package com.hospitalflow.labpharmacy;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "medicine_details", nullable = false, columnDefinition = "TEXT")
    private String medicineDetails;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    // Nullable: prescriptions dispensed before inventory tracking have no audit fields
    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "dispensed_by", length = 100)
    private String dispensedBy;

    public enum Status { PENDING, DISPENSED }

    public Prescription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getMedicineDetails() { return medicineDetails; }
    public void setMedicineDetails(String medicineDetails) { this.medicineDetails = medicineDetails; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(LocalDateTime dispensedAt) { this.dispensedAt = dispensedAt; }
    public String getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(String dispensedBy) { this.dispensedBy = dispensedBy; }
}
