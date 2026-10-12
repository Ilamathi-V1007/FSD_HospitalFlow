package com.hospitalflow.queue;

import com.hospitalflow.simulation.PriorityScorer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueEntryRepository queueEntryRepository;
    private final PatientRepository patientRepository;

    public QueueController(QueueEntryRepository queueEntryRepository, PatientRepository patientRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.patientRepository = patientRepository;
    }

    /** Register a new patient */
    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<Patient> registerPatient(@Valid @RequestBody RegisterPatientRequest req) {
        Patient p = new Patient();
        p.setName(req.name());
        p.setAge(req.age());
        p.setGender(req.gender());
        p.setContact(req.contact());
        p.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(patientRepository.save(p));
    }

    /** Search existing patients by name */
    @GetMapping("/patients/search")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public List<Patient> searchPatients(@RequestParam(defaultValue = "") String name) {
        if (name == null || name.isBlank()) {
            return patientRepository.findAll();
        }
        return patientRepository.findByNameContainingIgnoreCase(name);
    }

    /** Check-in patient into queue with urgency level and symptoms */
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<QueueItemDto> checkIn(@Valid @RequestBody CheckInRequest req) {
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + req.patientId()));

        QueueEntry entry = new QueueEntry();
        entry.setPatient(patient);
        entry.setSymptoms(req.symptoms());
        entry.setUrgencyLevel(req.urgencyLevel());
        entry.setStatus(QueueEntry.Status.WAITING);
        entry.setCheckedInAt(LocalDateTime.now());

        // Initial priority score: wait minutes is 0
        double initialScore = PriorityScorer.score(req.urgencyLevel(), 0.0);
        entry.setPriorityScore(initialScore);

        QueueEntry saved = queueEntryRepository.save(entry);
        return ResponseEntity.ok(toDto(saved));
    }

    /** View waiting queue sorted by priority score (re-scored by elapsed time) */
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public List<QueueItemDto> getWaitingQueue() {
        List<QueueEntry> waiting = queueEntryRepository.findByStatusOrderByPriorityScoreDesc(QueueEntry.Status.WAITING);
        LocalDateTime now = LocalDateTime.now();

        // Dynamically update scores based on wait duration so priority is live and accurate
        for (QueueEntry entry : waiting) {
            double waitMinutes = Duration.between(entry.getCheckedInAt(), now).toMinutes();
            double updatedScore = PriorityScorer.score(entry.getUrgencyLevel(), waitMinutes);
            entry.setPriorityScore(updatedScore);
            queueEntryRepository.save(entry);
        }

        return queueEntryRepository.findByStatusOrderByPriorityScoreDesc(QueueEntry.Status.WAITING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private QueueItemDto toDto(QueueEntry e) {
        double waitMins = Duration.between(e.getCheckedInAt(), LocalDateTime.now()).toMinutes();
        return new QueueItemDto(
                e.getId(),
                e.getPatient().getId(),
                e.getPatient().getName(),
                e.getPatient().getAge(),
                e.getPatient().getGender(),
                e.getPatient().getContact(),
                e.getSymptoms(),
                e.getUrgencyLevel(),
                e.getStatus().name(),
                e.getCheckedInAt(),
                e.getPriorityScore(),
                (long) waitMins
        );
    }

    // ─── DTO Records ─────────────────────────────────────────────────────────────

    public record RegisterPatientRequest(
            @NotBlank String name,
            Integer age,
            String gender,
            String contact) {}

    public record CheckInRequest(
            @NotNull Long patientId,
            String symptoms,
            @NotNull @Min(1) @Max(4) Integer urgencyLevel) {}

    public record QueueItemDto(
            Long queueId,
            Long patientId,
            String patientName,
            Integer age,
            String gender,
            String contact,
            String symptoms,
            Integer urgencyLevel,
            String status,
            LocalDateTime checkedInAt,
            Double priorityScore,
            Long waitMinutes) {}
}

