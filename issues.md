# FlowAlloc System Improvements & Audit Log

## Functional Fix 1: Consultation Completion & Queue Advancement
- **Root Cause**: In `AllocationController.java` (`completeConsultation`), when a doctor concluded an appointment, the method updated the doctor status and queue entry status to `COMPLETED`, but failed to update `appt.setStatus(Appointment.Status.COMPLETED)` and `appt.setEndedAt(LocalDateTime.now())` on the Appointment entity itself. Because `callNext` checks `appointmentRepository.findFirstByDoctorAndStatus(doctor, Appointment.Status.IN_PROGRESS).isPresent()`, the doctor was permanently flagged as having an active consultation in progress, which blocked claiming any subsequent waiting patient.
- **Resolution**:
  - In `AllocationController.java`, added `appt.setStatus(Appointment.Status.COMPLETED); appt.setEndedAt(LocalDateTime.now()); appointmentRepository.save(appt);`.
  - In `LabPharmacyController.java`, updated `getReadyForDischarge` and `discharge` to check `visitSummaryRepository.existsByAppointmentId(appt.getId())` rather than requiring `status == IN_PROGRESS`, ensuring patients with completed consultations remain discharge-eligible while waiting for diagnostic wrap-up and discharge.
  - In `VisitSummaryRepository.java`, added `boolean existsByAppointmentId(Long appointmentId);`.

## Functional Fix 2: Admin-Only Multi-Staff Account Creation
- **Backend Architecture**:
  - In `SecurityConfig.java`, enforced `.requestMatchers("/api/auth/staff/**").hasRole("ADMIN")` to prevent public or non-admin account registration.
  - In `AuthController.java`, added `@PostMapping("/staff")` and `@GetMapping("/staff")` secured with `@PreAuthorize("hasRole('ADMIN')")`.
  - In `AuthService.java`, created `createStaffUser` and `getAllStaff` returning `StaffUserDto` (omitting sensitive hashes). Added automatic provisioning of `Doctor` entity upon `DOCTOR` role creation so new doctors can immediately log in and call patients.
- **Frontend Architecture**:
  - In `AdminDashboard.jsx`, added an interactive **Staff Directory & Account Management** ledger with a modal dialog for creating individual staff credentials (Full Name, Email/Username, Password with visibility toggle, Confirm Password, Role selection).

## Issue 1: Hero Image Layout
- **Root Cause**: The hero section was rendering a separate card with an emoji and fallback text when the external image reference was unresolved, appearing as a floating placeholder card instead of a full-bleed hospital background photograph.
- **Resolution**:
  - Integrated the official high-resolution asset `Focused Clinician in Modern Hospital Ward.png` directly into `LandingPage.jsx`.
  - Replaced the card container with a full-bleed, seamless background element using `background-size: cover` and `background-position: center 25%`, with elegant border radius and subtle shadow.
  - Added responsive rules in `index.css` for clean stacking at mobile viewports (<900px and <600px).

## Issue 2: Login Failure for All Roles
- **Root Cause**:
  1. Port mismatch: `application.yml` previously defaulted to `server.port: 8080` while the frontend was configured to call `http://localhost:8082/api`. When launched without explicit CLI argument `--server.port=8082`, the backend listened on 8080 and rejected/refused all frontend requests.
  2. In `DemoDataSeeder.java`, `createUserIfAbsent` only inserted users if absent; if users had previously existed in the database with outdated or different password hashes, they remained unable to authenticate with `Demo@1234`.
- **Resolution**:
  - Configured `server.port: ${PORT:8082}` in `application.yml` to guarantee backend always listens on port 8082 by default.
  - Updated `DemoDataSeeder.java` so that on startup, existing demo users have their password hash securely verified and reset to `Demo@1234` if mismatched, guaranteeing immediate login across all 5 roles.
