package com.hospitalflow.allocation;

import com.hospitalflow.auth.User;
import com.hospitalflow.auth.UserRepository;
import com.hospitalflow.queue.Patient;
import com.hospitalflow.queue.PatientRepository;
import com.hospitalflow.queue.QueueEntry;
import com.hospitalflow.queue.QueueEntryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/allocation")
public class AllocationController {

    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public AllocationController(
            BedRepository bedRepository,
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            QueueEntryRepository queueEntryRepository,
            PatientRepository patientRepository,
            UserRepository userRepository) {
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    /** List all hospital beds */
    @GetMapping("/beds")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'RECEPTIONIST')")
    public List<Bed> getAllBeds() {
        return bedRepository.findAll();
    }

    /** Create a new bed */
    @PostMapping("/beds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Bed> createBed(@RequestBody Bed bed) {
        if (bed.getStatus() == null) {
            bed.setStatus(Bed.Status.AVAILABLE);
        }
        return ResponseEntity.ok(bedRepository.save(bed));
    }

    /** Get current doctor profile & state for the authenticated doctor */
    @GetMapping("/doctor/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorStateDto> getMyDoctorState(Authentication auth) {
        Doctor doctor = getOrCreateDoctorForUser(auth.getName());
        Appointment activeAppt = appointmentRepository.findFirstByDoctorAndStatus(doctor, Appointment.Status.IN_PROGRESS).orElse(null);
        ActiveConsultationDto activeDto = activeAppt != null ? toActiveConsultationDto(activeAppt) : null;

        return ResponseEntity.ok(new DoctorStateDto(
                doctor.getId(),
                auth.getName(),
                doctor.getDepartment(),
                doctor.getAvailabilityStatus().name(),
                activeDto
        ));
    }

    /** Toggle or set doctor status (AVAILABLE, ON_BREAK) */
    @PostMapping("/doctor/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorStateDto> updateDoctorStatus(
            Authentication auth,
            @RequestBody UpdateStatusRequest req) {
        Doctor doctor = getOrCreateDoctorForUser(auth.getName());
        try {
            Doctor.AvailabilityStatus newStatus = Doctor.AvailabilityStatus.valueOf(req.status().toUpperCase());
            doctor.setAvailabilityStatus(newStatus);
            doctorRepository.save(doctor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + req.status());
        }
        return getMyDoctorState(auth);
    }

    /** Call next patient: claims highest-priority WAITING patient atomically */
    @PostMapping("/call-next")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public ResponseEntity<ActiveConsultationDto> callNext(Authentication auth) {
        Doctor doctor = getOrCreateDoctorForUser(auth.getName());

        // Check if doctor already has an active consultation
        if (appointmentRepository.findFirstByDoctorAndStatus(doctor, Appointment.Status.IN_PROGRESS).isPresent()) {
            throw new IllegalArgumentException("You already have an active consultation in progress. Complete it first.");
        }

        // Fetch top waiting patient
        List<QueueEntry> waiting = queueEntryRepository.findByStatusOrderByPriorityScoreDesc(QueueEntry.Status.WAITING);
        if (waiting.isEmpty()) {
            throw new IllegalArgumentException("No patients currently waiting in queue.");
        }

        QueueEntry topEntry = waiting.get(0);
        topEntry.setStatus(QueueEntry.Status.IN_CONSULT);
        queueEntryRepository.save(topEntry);

        // Mark doctor BUSY
        doctor.setAvailabilityStatus(Doctor.AvailabilityStatus.BUSY);
        doctorRepository.save(doctor);

        // Create new Appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(topEntry.getPatient().getId());
        appointment.setDoctor(doctor);
        appointment.setStatus(Appointment.Status.IN_PROGRESS);
        appointment.setStartedAt(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(appointment);

        return ResponseEntity.ok(toActiveConsultationDto(saved, topEntry.getSymptoms(), topEntry.getUrgencyLevel()));
    }

    /** Assign bed to appointment */
    @PostMapping("/appointments/{appointmentId}/assign-bed")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Transactional
    public ResponseEntity<ActiveConsultationDto> assignBed(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AssignBedRequest req) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        Bed bed = bedRepository.findById(req.bedId())
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + req.bedId()));

        if (bed.getStatus() == Bed.Status.OCCUPIED) {
            throw new IllegalArgumentException("Bed " + bed.getBedNumber() + " is already occupied!");
        }

        // Release old bed if any
        if (appt.getBed() != null) {
            Bed oldBed = appt.getBed();
            oldBed.setStatus(Bed.Status.AVAILABLE);
            bedRepository.save(oldBed);
        }

        bed.setStatus(Bed.Status.OCCUPIED);
        bedRepository.save(bed);

        appt.setBed(bed);
        Appointment saved = appointmentRepository.save(appt);
        return ResponseEntity.ok(toActiveConsultationDto(saved));
    }

    /** Mark consultation complete */
    @PostMapping("/appointments/{appointmentId}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public ResponseEntity<String> completeConsultation(
            @PathVariable Long appointmentId,
            Authentication auth) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        Doctor doctor = getOrCreateDoctorForUser(auth.getName());
        doctor.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
        doctorRepository.save(doctor);

        // Mark consultation appointment complete
        appt.setStatus(Appointment.Status.COMPLETED);
        appt.setEndedAt(LocalDateTime.now());
        appointmentRepository.save(appt);

        // Queue entry completion
        List<QueueEntry> entries = queueEntryRepository.findAll();
        for (QueueEntry q : entries) {
            if (q.getPatient().getId().equals(appt.getPatientId()) && q.getStatus() == QueueEntry.Status.IN_CONSULT) {
                q.setStatus(QueueEntry.Status.COMPLETED);
                queueEntryRepository.save(q);
                break;
            }
        }

        return ResponseEntity.ok("Consultation completed. Patient is ready for discharge or lab tests.");
    }

    // ─── Helpers & DTOs ──────────────────────────────────────────────────────────

    private Doctor getOrCreateDoctorForUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return doctorRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Doctor d = new Doctor();
                    d.setUserId(user.getId());
                    d.setDepartment("General OPD");
                    d.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
                    return doctorRepository.save(d);
                });
    }

    private ActiveConsultationDto toActiveConsultationDto(Appointment a) {
        return toActiveConsultationDto(a, null, null);
    }

    private ActiveConsultationDto toActiveConsultationDto(Appointment a, String symptoms, Integer urgencyLevel) {
        Patient p = patientRepository.findById(a.getPatientId()).orElse(null);
        String pName = p != null ? p.getName() : "Unknown";
        Integer pAge = p != null ? p.getAge() : null;
        String pGender = p != null ? p.getGender() : null;
        String pContact = p != null ? p.getContact() : null;

        String bedNum = a.getBed() != null ? a.getBed().getBedNumber() : null;
        Long bedId = a.getBed() != null ? a.getBed().getId() : null;

        return new ActiveConsultationDto(
                a.getId(),
                a.getPatientId(),
                pName,
                pAge,
                pGender,
                pContact,
                symptoms,
                urgencyLevel,
                a.getStatus().name(),
                bedId,
                bedNum,
                a.getStartedAt()
        );
    }

    public record UpdateStatusRequest(String status) {}

    public record AssignBedRequest(@NotNull Long bedId) {}

    public record DoctorStateDto(
            Long doctorId,
            String doctorEmail,
            String department,
            String availabilityStatus,
            ActiveConsultationDto activeConsultation) {}

    public record ActiveConsultationDto(
            Long appointmentId,
            Long patientId,
            String patientName,
            Integer age,
            String gender,
            String contact,
            String symptoms,
            Integer urgencyLevel,
            String status,
            Long bedId,
            String bedNumber,
            LocalDateTime startedAt) {}
}

