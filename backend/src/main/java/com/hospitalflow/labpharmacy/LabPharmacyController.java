package com.hospitalflow.labpharmacy;

import com.hospitalflow.allocation.Appointment;
import com.hospitalflow.allocation.AppointmentRepository;
import com.hospitalflow.allocation.Bed;
import com.hospitalflow.allocation.BedRepository;
import com.hospitalflow.inventory.InventoryDtos.DispenseRequest;
import com.hospitalflow.inventory.InventoryService;
import com.hospitalflow.queue.Patient;
import com.hospitalflow.queue.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/labpharmacy")
public class LabPharmacyController {

    private final LabRequestRepository labRequestRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final InventoryService inventoryService;

    public LabPharmacyController(
            LabRequestRepository labRequestRepository,
            PrescriptionRepository prescriptionRepository,
            VisitSummaryRepository visitSummaryRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            BedRepository bedRepository,
            InventoryService inventoryService) {
        this.labRequestRepository = labRequestRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.visitSummaryRepository = visitSummaryRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.inventoryService = inventoryService;
    }

    // ─── LAB ENDPOINTS ──────────────────────────────────────────────────────────

    /** All pending lab requests — for Lab Technician dashboard */
    @GetMapping("/lab-requests/pending")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public List<LabRequestDto> getPendingLabRequests() {
        return labRequestRepository.findByStatus(LabRequest.Status.PENDING)
                .stream().map(this::toLabDto).collect(Collectors.toList());
    }

    /** All completed lab requests — for Lab Technician history */
    @GetMapping("/lab-requests/completed")
    @PreAuthorize("hasAnyRole('LAB_TECH', 'ADMIN')")
    public List<LabRequestDto> getCompletedLabRequests() {
        return labRequestRepository.findByStatus(LabRequest.Status.COMPLETED)
                .stream().map(this::toLabDto).collect(Collectors.toList());
    }

    /** Doctor creates a new lab request for an appointment */
    @PostMapping("/lab-requests")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LabRequestDto> createLabRequest(@RequestBody CreateLabOrderRequest body) {
        Appointment appt = appointmentRepository.findById(body.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + body.appointmentId()));

        LabRequest req = new LabRequest();
        req.setAppointmentId(appt.getId());
        req.setTestName(body.testName());
        req.setStatus(LabRequest.Status.PENDING);
        req.setRequestedAt(LocalDateTime.now());
        return ResponseEntity.ok(toLabDto(labRequestRepository.save(req)));
    }

    /** Lab technician enters result and marks request complete */
    @PostMapping("/lab-requests/{id}/complete")
    @PreAuthorize("hasRole('LAB_TECH')")
    public ResponseEntity<LabRequestDto> completeLabRequest(
            @PathVariable Long id,
            @RequestBody CompleteLabRequest body) {
        LabRequest req = labRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lab request not found"));
        if (req.getStatus() == LabRequest.Status.COMPLETED) {
            throw new IllegalArgumentException("Lab request is already completed");
        }
        req.setResult(body.result());
        req.setStatus(LabRequest.Status.COMPLETED);
        return ResponseEntity.ok(toLabDto(labRequestRepository.save(req)));
    }

    // ─── PHARMACY ENDPOINTS ──────────────────────────────────────────────────────

    /** Pending prescriptions — for Pharmacist dashboard */
    @GetMapping("/prescriptions/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<PrescriptionDto> getPendingPrescriptions() {
        return prescriptionRepository.findByStatus(Prescription.Status.PENDING)
                .stream().map(this::toPrescriptionDto).collect(Collectors.toList());
    }

    /** Recently dispensed prescriptions — for Pharmacist history */
    @GetMapping("/prescriptions/dispensed")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<PrescriptionDto> getDispensedPrescriptions() {
        return prescriptionRepository.findByStatus(Prescription.Status.DISPENSED)
                .stream().map(this::toPrescriptionDto).collect(Collectors.toList());
    }

    /** Doctor writes a new prescription for an appointment */
    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PrescriptionDto> createPrescription(@RequestBody CreatePrescriptionOrderRequest body) {
        Appointment appt = appointmentRepository.findById(body.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + body.appointmentId()));

        Prescription p = new Prescription();
        p.setAppointmentId(appt.getId());
        p.setMedicineDetails(body.medicineDetails());
        p.setStatus(Prescription.Status.PENDING);
        return ResponseEntity.ok(toPrescriptionDto(prescriptionRepository.save(p)));
    }

    /**
     * Pharmacist marks a prescription as dispensed. The body lists the inventory medicines and
     * quantities handed over; stock is checked and deducted in the same transaction (see InventoryService).
     */
    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<PrescriptionDto> dispensePrescription(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DispenseRequest body,
            Authentication auth) {
        Prescription p = inventoryService.dispense(id, body, auth.getName());
        return ResponseEntity.ok(toPrescriptionDto(p));
    }

    // ─── DISCHARGE ENDPOINTS ─────────────────────────────────────────────────────

    /**
     * Appointments that are ready to discharge:
     *  – status IN_PROGRESS
     *  – all lab requests (if any) are COMPLETED
     */
    @GetMapping("/discharge/ready")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public List<DischargeReadyDto> getReadyForDischarge() {
        List<Appointment> candidates = appointmentRepository.findAll().stream()
                .filter(appt -> !visitSummaryRepository.existsByAppointmentId(appt.getId()))
                .collect(Collectors.toList());

        List<DischargeReadyDto> result = new ArrayList<>();
        for (Appointment appt : candidates) {
            List<LabRequest> labs = labRequestRepository.findByAppointmentId(appt.getId());
            boolean allLabsDone = labs.isEmpty() ||
                    labs.stream().allMatch(l -> l.getStatus() == LabRequest.Status.COMPLETED);
            if (allLabsDone) {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                String patientName = patient != null ? patient.getName() : "Unknown";
                String bedNumber = appt.getBed() != null ? appt.getBed().getBedNumber() : null;
                result.add(new DischargeReadyDto(
                        appt.getId(), patientName, appt.getStartedAt(),
                        labs.size(), bedNumber));
            }
        }
        return result;
    }

    /**
     * Discharge a patient: write visit summary, mark appointment COMPLETED,
     * free bed if assigned — all in one transaction.
     */
    @PostMapping("/discharge/{appointmentId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<VisitSummaryDto> discharge(
            @PathVariable Long appointmentId,
            @RequestBody DischargeRequest body) {

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (visitSummaryRepository.existsByAppointmentId(appointmentId)) {
            throw new IllegalArgumentException("Appointment is already discharged");
        }

        // Check all labs done
        List<LabRequest> labs = labRequestRepository.findByAppointmentId(appointmentId);
        boolean allDone = labs.stream().allMatch(l -> l.getStatus() == LabRequest.Status.COMPLETED);
        if (!allDone) {
            throw new IllegalArgumentException("Cannot discharge: pending lab requests remain");
        }

        // Write visit summary
        VisitSummary summary = new VisitSummary();
        summary.setAppointmentId(appointmentId);
        summary.setDiagnosis(body.diagnosis());
        summary.setDischargedAt(LocalDateTime.now());
        visitSummaryRepository.save(summary);

        // Close appointment
        appt.setStatus(Appointment.Status.COMPLETED);
        appt.setEndedAt(LocalDateTime.now());

        // Free bed
        if (appt.getBed() != null) {
            Bed bed = appt.getBed();
            bed.setStatus(Bed.Status.AVAILABLE);
            bedRepository.save(bed);
            appt.setBed(null);
        }
        appointmentRepository.save(appt);

        Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getName() : "Unknown";

        return ResponseEntity.ok(new VisitSummaryDto(
                summary.getId(), appointmentId, patientName,
                summary.getDiagnosis(), summary.getDischargedAt()));
    }

    // ─── DTO helpers ─────────────────────────────────────────────────────────────

    private LabRequestDto toLabDto(LabRequest r) {
        Appointment appt = appointmentRepository.findById(r.getAppointmentId()).orElse(null);
        String patientName = "Unknown";
        if (appt != null) {
            Patient p = patientRepository.findById(appt.getPatientId()).orElse(null);
            if (p != null) patientName = p.getName();
        }
        return new LabRequestDto(r.getId(), r.getAppointmentId(), patientName,
                r.getTestName(), r.getStatus().name(), r.getResult(), r.getRequestedAt());
    }

    private PrescriptionDto toPrescriptionDto(Prescription p) {
        Appointment appt = appointmentRepository.findById(p.getAppointmentId()).orElse(null);
        String patientName = "Unknown";
        if (appt != null) {
            Patient pat = patientRepository.findById(appt.getPatientId()).orElse(null);
            if (pat != null) patientName = pat.getName();
        }
        return new PrescriptionDto(p.getId(), p.getAppointmentId(), patientName,
                p.getMedicineDetails(), p.getStatus().name());
    }

    // ─── Records (DTOs) ──────────────────────────────────────────────────────────

    public record LabRequestDto(
            Long id, Long appointmentId, String patientName,
            String testName, String status, String result,
            java.time.LocalDateTime requestedAt) {}

    public record CompleteLabRequest(String result) {}

    public record PrescriptionDto(
            Long id, Long appointmentId, String patientName,
            String medicineDetails, String status) {}

    public record CreateLabOrderRequest(Long appointmentId, String testName) {}

    public record CreatePrescriptionOrderRequest(Long appointmentId, String medicineDetails) {}

    public record DischargeReadyDto(
            Long appointmentId, String patientName,
            java.time.LocalDateTime startedAt, int labCount, String bedNumber) {}

    public record DischargeRequest(String diagnosis) {}

    public record VisitSummaryDto(
            Long summaryId, Long appointmentId, String patientName,
            String diagnosis, java.time.LocalDateTime dischargedAt) {}
}
