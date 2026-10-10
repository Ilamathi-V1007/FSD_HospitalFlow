package com.hospitalflow.labpharmacy;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visit_summaries")
public class VisitSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt = LocalDateTime.now();

    public VisitSummary() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public LocalDateTime getDischargedAt() { return dischargedAt; }
    public void setDischargedAt(LocalDateTime dischargedAt) { this.dischargedAt = dischargedAt; }
}
