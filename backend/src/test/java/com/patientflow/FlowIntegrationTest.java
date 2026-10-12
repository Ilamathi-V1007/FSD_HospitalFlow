package com.patientflow;

import com.patientflow.allocation.AllocationDtos.CompleteRequest;
import com.patientflow.allocation.AllocationDtos.DoctorState;
import com.patientflow.allocation.BedAssignmentRepository;
import com.patientflow.allocation.BedRepository;
import com.patientflow.allocation.BedService;
import com.patientflow.allocation.BedStatus;
import com.patientflow.allocation.ConsultationService;
import com.patientflow.allocation.DoctorService;
import com.patientflow.allocation.DoctorStatus;
import com.patientflow.auth.User;
import com.patientflow.auth.UserRepository;
import com.patientflow.common.AuthUser;
import com.patientflow.labpharmacy.DischargeService;
import com.patientflow.labpharmacy.LabService;
import com.patientflow.labpharmacy.PrescriptionService;
import com.patientflow.queue.Patient;
import com.patientflow.queue.PatientRepository;
import com.patientflow.queue.QueueDtos.CheckInRequest;
import com.patientflow.queue.QueueDtos.PatientDto;
import com.patientflow.queue.QueueDtos.PatientRequest;
import com.patientflow.queue.QueueService;
import com.patientflow.queue.Visit;
import com.patientflow.queue.VisitRepository;
import com.patientflow.queue.VisitStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared helpers for tests that exercise the real services against PostgreSQL.
 *
 * <p>All integration tests share one database, so every test starts and ends from a known state:
 * empty waiting queue, no occupied beds, and both seeded doctors AVAILABLE with no open consultation.
 */
public abstract class FlowIntegrationTest extends PostgresIntegrationTest {

    protected static final String DOCTOR_1 = "doctor1@patientflow.local";
    protected static final String DOCTOR_2 = "doctor2@patientflow.local";
    protected static final String LAB_TECH = "lab@patientflow.local";

    @Autowired
    protected UserRepository users;
    @Autowired
    protected QueueService queue;
    @Autowired
    protected ConsultationService consultations;
    @Autowired
    protected DoctorService doctorService;
    @Autowired
    protected BedService bedService;
    @Autowired
    protected BedRepository bedRepository;
    @Autowired
    protected BedAssignmentRepository assignmentRepository;
    @Autowired
    protected LabService labService;
    @Autowired
    protected PrescriptionService prescriptionService;
    @Autowired
    protected DischargeService dischargeService;
    @Autowired
    protected PatientRepository patientRepository;
    @Autowired
    protected VisitRepository visitRepository;

    @BeforeEach
    protected void cleanSharedStateBefore() {
        resetSharedState();
    }

    @AfterEach
    protected void cleanSharedStateAfter() {
        resetSharedState();
    }

    private void resetSharedState() {
        queue.getQueue().forEach(entry -> queue.cancel(entry.visitId()));
        for (String email : List.of(DOCTOR_1, DOCTOR_2)) {
            AuthUser doctor = auth(email);
            DoctorState state = consultations.current(doctor);
            if (state.activeConsultation() != null) {
                consultations.complete(doctor, state.activeConsultation().id(), new CompleteRequest("test cleanup", null));
            }
            doctorService.setStatus(doctor, DoctorStatus.AVAILABLE);
        }
        bedRepository.findAll().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                .forEach(b -> bedService.release(b.getId()));
    }

    protected AuthUser auth(String email) {
        User u = users.findByEmailIgnoreCase(email).orElseThrow();
        return new AuthUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole());
    }

    /** A new patient checked in through the real service, so it appears in the waiting queue. */
    protected Long checkIn(String name, int triageLevel) {
        PatientDto patient = queue.registerPatient(new PatientRequest(name, 35, "Other", "9000000000"));
        return queue.checkIn(new CheckInRequest(patient.id(), "test symptoms", triageLevel)).visitId();
    }

    /** A new patient whose visit is put straight into the given status (bypasses the queue). */
    protected Long visitWithStatus(String name, VisitStatus status) {
        Patient p = new Patient();
        p.setFullName(name);
        p.setAge(30);
        p.setGender("Other");
        p = patientRepository.save(p);
        Visit v = new Visit();
        v.setPatientId(p.getId());
        v.setSymptoms("test symptoms");
        v.setTriageLevel(2);
        v.setStatus(status);
        v.setCheckInTime(Instant.now());
        return visitRepository.save(v).getId();
    }

    /** Ids of beds that are currently free, in label order. */
    protected List<Long> availableBedIds() {
        return bedRepository.findAllByOrderByLabelAsc().stream()
                .filter(b -> b.getStatus() == BedStatus.AVAILABLE)
                .map(b -> b.getId())
                .toList();
    }

    /**
     * Runs all tasks at the same moment on separate threads. Returns one entry per task, in order:
     * null if the task succeeded, otherwise whatever it threw.
     */
    protected List<Throwable> runConcurrently(List<Runnable> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        for (Runnable task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    task.run();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            }));
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        List<Throwable> outcomes = new ArrayList<>();
        for (Future<Throwable> f : futures) {
            outcomes.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return outcomes;
    }
}
