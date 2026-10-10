package com.hospitalflow.labpharmacy;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_requests")
public class LabRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain FK id since Appointment lives in the allocation module
    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();

    public enum Status { PENDING, COMPLETED }

    public LabRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
