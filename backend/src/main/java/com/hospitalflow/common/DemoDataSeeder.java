package com.hospitalflow.common;

import com.hospitalflow.allocation.Appointment;
import com.hospitalflow.allocation.AppointmentRepository;
import com.hospitalflow.allocation.Bed;
import com.hospitalflow.allocation.BedRepository;
import com.hospitalflow.allocation.Doctor;
import com.hospitalflow.allocation.DoctorRepository;
import com.hospitalflow.auth.User;
import com.hospitalflow.auth.UserRepository;
import com.hospitalflow.labpharmacy.LabRequest;
import com.hospitalflow.labpharmacy.LabRequestRepository;
import com.hospitalflow.labpharmacy.Prescription;
import com.hospitalflow.labpharmacy.PrescriptionRepository;
import com.hospitalflow.labpharmacy.VisitSummary;
import com.hospitalflow.labpharmacy.VisitSummaryRepository;
import com.hospitalflow.queue.Patient;
import com.hospitalflow.queue.PatientRepository;
import com.hospitalflow.queue.QueueEntry;
import com.hospitalflow.queue.QueueEntryRepository;
import com.hospitalflow.simulation.PriorityScorer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Idempotent, safe synthetic demo data seeder.
 * Runs on startup and populates baseline users, beds, patients, queue entries,
 * lab requests, and prescriptions if not already present.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabRequestRepository labRequestRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            BedRepository bedRepository,
            PatientRepository patientRepository,
            QueueEntryRepository queueEntryRepository,
            AppointmentRepository appointmentRepository,
            LabRequestRepository labRequestRepository,
            PrescriptionRepository prescriptionRepository,
            VisitSummaryRepository visitSummaryRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.appointmentRepository = appointmentRepository;
        this.labRequestRepository = labRequestRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.visitSummaryRepository = visitSummaryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsersAndDoctors();
        seedBeds();
        seedPatientsAndWorkflows();
    }

    private void seedUsersAndDoctors() {
        // Standard Demo Staff Accounts with Demo@1234
        createUserIfAbsent("Admin Lead", "admin@patientflow.local", User.Role.ADMIN);
        createUserIfAbsent("Reception Desk", "reception@patientflow.local", User.Role.RECEPTIONIST);
        User doc = createUserIfAbsent("Dr. Aris Thorne", "doctor@patientflow.local", User.Role.DOCTOR);
        User doc1 = createUserIfAbsent("Dr. Aris Thorne", "doctor1@patientflow.local", User.Role.DOCTOR);
        User doc2 = createUserIfAbsent("Dr. Maya Lin", "doctor2@patientflow.local", User.Role.DOCTOR);
        createUserIfAbsent("Clinical Lab", "lab@patientflow.local", User.Role.LAB_TECH);
        createUserIfAbsent("Central Pharmacy", "pharmacy@patientflow.local", User.Role.PHARMACIST);

        if (doc != null && doctorRepository.findByUserId(doc.getId()).isEmpty()) {
            Doctor d = new Doctor();
            d.setUserId(doc.getId());
            d.setDepartment("Emergency OPD");
            d.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
            doctorRepository.save(d);
        }

        if (doc1 != null && doctorRepository.findByUserId(doc1.getId()).isEmpty()) {
            Doctor d = new Doctor();
            d.setUserId(doc1.getId());
            d.setDepartment("Emergency OPD");
            d.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
            doctorRepository.save(d);
        }

        if (doc2 != null && doctorRepository.findByUserId(doc2.getId()).isEmpty()) {
            Doctor d = new Doctor();
            d.setUserId(doc2.getId());
            d.setDepartment("Internal Medicine");
            d.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
            doctorRepository.save(d);
        }
    }

    private User createUserIfAbsent(String name, String email, User.Role role) {
        return userRepository.findByEmailIgnoreCase(email).map(existing -> {
            if (!passwordEncoder.matches("Demo@1234", existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode("Demo@1234"));
                existing.setRole(role);
                return userRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            User u = new User();
            u.setName(name);
            u.setEmail(email.toLowerCase());
            u.setPasswordHash(passwordEncoder.encode("Demo@1234"));
            u.setRole(role);
            return userRepository.save(u);
        });
    }

    private void seedBeds() {
        String[] bedNumbers = {"101", "102", "103", "104", "105", "106", "201", "202"};
        for (String num : bedNumbers) {
            boolean exists = bedRepository.findAll().stream().anyMatch(b -> b.getBedNumber().equals(num));
            if (!exists) {
                Bed b = new Bed();
                b.setBedNumber(num);
                b.setStatus(Bed.Status.AVAILABLE);
                bedRepository.save(b);
            }
        }
    }

    private void seedPatientsAndWorkflows() {
        if (patientRepository.count() > 0) {
            return; // Seeded data already exists; maintain idempotency
        }

        // 1. Seed Patients
        Patient p1 = createPatient("Elena Rostova", 58, "Female", "+91 9845012341");
        Patient p2 = createPatient("David Kim", 34, "Male", "+91 9845012342");
        Patient p3 = createPatient("Priya Nair", 27, "Female", "+91 9845012343");
        Patient p4 = createPatient("Samuel O'Connor", 72, "Male", "+91 9845012344");
        Patient p5 = createPatient("Amara Diallo", 41, "Female", "+91 9845012345");
        Patient p6 = createPatient("Marcus Vance", 63, "Male", "+91 9845012346");

        LocalDateTime now = LocalDateTime.now();

        // 2. Seed Live Waiting Queue Entries with different triage levels
        createQueueEntry(p1, "Acute chest tightness radiating to left jaw", 1, now.minusMinutes(12));
        createQueueEntry(p2, "Severe abdominal pain with high fever", 2, now.minusMinutes(25));
        createQueueEntry(p3, "Suspected radial fracture post fall", 3, now.minusMinutes(45));
        createQueueEntry(p4, "Chronic knee discomfort and prescription renewal", 4, now.minusMinutes(75));

        // 3. Seed an active in-consultation appointment for Doctor 1
        User doc1User = userRepository.findByEmail("doctor1@patientflow.local").orElse(null);
        if (doc1User != null) {
            Doctor doc1 = doctorRepository.findByUserId(doc1User.getId()).orElse(null);
            if (doc1 != null) {
                Bed bed101 = bedRepository.findAll().stream().filter(b -> "101".equals(b.getBedNumber())).findFirst().orElse(null);
                if (bed101 != null) {
                    bed101.setStatus(Bed.Status.OCCUPIED);
                    bedRepository.save(bed101);
                }

                Appointment activeAppt = new Appointment();
                activeAppt.setPatientId(p5.getId());
                activeAppt.setDoctor(doc1);
                activeAppt.setBed(bed101);
                activeAppt.setStatus(Appointment.Status.IN_PROGRESS);
                activeAppt.setStartedAt(now.minusMinutes(20));
                Appointment savedAppt = appointmentRepository.save(activeAppt);

                // Create Queue entry marked IN_CONSULT
                QueueEntry qe5 = new QueueEntry();
                qe5.setPatient(p5);
                qe5.setSymptoms("Dyspnea and oxygen desaturation (SpO2 91%)");
                qe5.setUrgencyLevel(1);
                qe5.setStatus(QueueEntry.Status.IN_CONSULT);
                qe5.setCheckedInAt(now.minusMinutes(35));
                qe5.setPriorityScore(PriorityScorer.score(1, 35.0));
                queueEntryRepository.save(qe5);

                // Add Pending Lab Request for Lab Tech Workspace
                LabRequest lab1 = new LabRequest();
                lab1.setAppointmentId(savedAppt.getId());
                lab1.setTestName("Stat ABG & High-Sensitivity Troponin");
                lab1.setStatus(LabRequest.Status.PENDING);
                lab1.setRequestedAt(now.minusMinutes(15));
                labRequestRepository.save(lab1);

                // Add Pending Prescription for Pharmacy Workspace
                Prescription rx1 = new Prescription();
                rx1.setAppointmentId(savedAppt.getId());
                rx1.setMedicineDetails("Supplemental O2 (4L/min nasal cannula), Salbutamol 2.5mg nebulization");
                rx1.setStatus(Prescription.Status.PENDING);
                prescriptionRepository.save(rx1);
            }
        }

        // 4. Seed a completed appointment with summary
        User doc2User = userRepository.findByEmail("doctor2@patientflow.local").orElse(null);
        if (doc2User != null) {
            Doctor doc2 = doctorRepository.findByUserId(doc2User.getId()).orElse(null);
            if (doc2 != null) {
                Appointment closedAppt = new Appointment();
                closedAppt.setPatientId(p6.getId());
                closedAppt.setDoctor(doc2);
                closedAppt.setStatus(Appointment.Status.COMPLETED);
                closedAppt.setStartedAt(now.minusHours(3));
                closedAppt.setEndedAt(now.minusHours(1));
                Appointment savedClosed = appointmentRepository.save(closedAppt);

                VisitSummary summary = new VisitSummary();
                summary.setAppointmentId(savedClosed.getId());
                summary.setDiagnosis("Hypertensive urgency stabilized. Commenced Amlodipine 5mg OD.");
                summary.setDischargedAt(now.minusHours(1));
                visitSummaryRepository.save(summary);

                // Completed lab
                LabRequest compLab = new LabRequest();
                compLab.setAppointmentId(savedClosed.getId());
                compLab.setTestName("Basic Metabolic Panel (BMP)");
                compLab.setStatus(LabRequest.Status.COMPLETED);
                compLab.setResult("Creatinine 1.0 mg/dL, Na+ 139 mEq/L, K+ 4.2 mEq/L (Normal)");
                compLab.setRequestedAt(now.minusHours(2).minusMinutes(30));
                labRequestRepository.save(compLab);

                // Dispensed prescription
                Prescription compRx = new Prescription();
                compRx.setAppointmentId(savedClosed.getId());
                compRx.setMedicineDetails("Amlodipine 5mg (1 tab OD x 30 days)");
                compRx.setStatus(Prescription.Status.DISPENSED);
                prescriptionRepository.save(compRx);
            }
        }
    }

    private Patient createPatient(String name, Integer age, String gender, String contact) {
        Patient p = new Patient();
        p.setName(name);
        p.setAge(age);
        p.setGender(gender);
        p.setContact(contact);
        p.setCreatedAt(LocalDateTime.now().minusHours(2));
        return patientRepository.save(p);
    }

    private void createQueueEntry(Patient patient, String symptoms, int urgencyLevel, LocalDateTime checkedInAt) {
        QueueEntry q = new QueueEntry();
        q.setPatient(patient);
        q.setSymptoms(symptoms);
        q.setUrgencyLevel(urgencyLevel);
        q.setStatus(QueueEntry.Status.WAITING);
        q.setCheckedInAt(checkedInAt);
        double waitMins = java.time.Duration.between(checkedInAt, LocalDateTime.now()).toMinutes();
        q.setPriorityScore(PriorityScorer.score(urgencyLevel, waitMins));
        queueEntryRepository.save(q);
    }
}
