# FlowAlloc — Dynamic Patient Flow and Resource Optimization System

**Right patient. Right resource. Right time.**

FlowAlloc is an intelligent hospital operations and patient-flow management platform designed to eliminate OPD bottlenecks, prevent resource double-booking, and prioritize acute emergency arrivals dynamically over first-come-first-served (FCFS) queuing.

---

## 1. Architecture & Port Reference

| Component | Port | Technology | Purpose |
|---|---|---|---|
| **Frontend Web App** | `5173` | React 19, Vite, React Router, Recharts | Role-specific clinical & administrative portals |
| **Backend REST API** | `8082` (local) / `8081` (docker) | Java 17, Spring Boot 3.3.4, Spring Security | Transactional clinical workflows & priority engine |
| **Database** | `5432` | PostgreSQL 16 | Relational persistence (`hospitalflow` schema) |
| **API Documentation** | `8082/swagger-ui.html` | SpringDoc OpenAPI | Live interactive REST endpoint documentation |

---

## 2. Verified Demo Accounts

Every demo account uses the secure password: **`Demo@1234`**

| Department / Role | Email Address | Portal Route | Primary Operations |
|---|---|---|---|
| **Administrator** | `admin@patientflow.local` | `/login/admin` | Hospital metrics, bed registry, FCFS vs. Priority simulation |
| **Receptionist** | `reception@patientflow.local` | `/login/receptionist` | Patient registration, triage category assignment (Levels 1–4), queue check-in |
| **Doctor** | `doctor1@patientflow.local` | `/login/doctor` | Call next prioritized patient, order lab tests, prescribe medication, assign beds |
| **Doctor (Alternative)** | `doctor2@patientflow.local` | `/login/doctor` | Secondary physician for concurrent allocation testing |
| **Laboratory** | `lab@patientflow.local` | `/login/lab` | Process incoming diagnostic orders, record clinical test values |
| **Pharmacy** | `pharmacy@patientflow.local` | `/login/pharmacy` | Review prescriptions, verify medication fulfillment, mark dispensed |

---

## 3. Local Startup Instructions

### Step 1: Start PostgreSQL Database
```powershell
# From the project root (D:\PatientFlowOptimizationSystem\FSD_HospitalFlow)
docker compose up -d postgres
```
*Note: If running PostgreSQL locally on Windows, ensure the service is active on port `5432` with a database named `hospitalflow`.*

### Step 2: Start Spring Boot Backend
```powershell
cd backend
mvn "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
```
The backend initializes the database and automatically runs `DemoDataSeeder` to populate synthetic patients, active appointments, beds, lab tests, and prescriptions if absent.

### Step 3: Start React Frontend
```powershell
cd ../frontend
npm install
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

---

## 4. End-to-End Demonstration Script

Follow this sequence for a college project evaluation or demonstration:

### Phase A: Landing Page & Role Selection
1. Open [http://localhost:5173](http://localhost:5173).
2. Observe the **FlowAlloc: Quiet Precision** visual identity and the 5 portal options.
3. Click **Receptionist** to navigate to `/login/receptionist`.

### Phase B: Intake & Triage (Receptionist)
1. Sign in with `reception@patientflow.local` / `Demo@1234`.
2. Under **1. Register Patient**, enter:
   - Full Name: `Vikramaditya Rao`
   - Age: `52`, Gender: `Male`, Contact: `+91 9845098765`
   - Click **Register Patient Record**.
3. Under **2. Triage & Queue Check-In**:
   - Select `Vikramaditya Rao`.
   - Select **Level 1 — Emergency (Immediate, Base 100)**.
   - Enter Symptoms: `Acute retrosternal chest pain radiating to left shoulder, diaphoresis`.
   - Click **Confirm Check-In to Queue**.
4. In the sidebar, click **Live Queue** (`/queue`):
   - Notice `Vikramaditya Rao` is dynamically placed at the top (`#1 [NEXT]`) with Priority Score `100.0`.
5. Click **Sign out**.

### Phase C: Examination & Orders (Doctor)
1. Select **Doctor** on the landing page (`/login/doctor`).
2. Sign in with `doctor1@patientflow.local` / `Demo@1234`.
3. Click **✦ Call Next Patient from Queue**.
4. Observe the state transition:
   - Status switches to **IN CONSULTATION**.
   - Vikramaditya Rao's clinical complaint is loaded.
5. **Order Laboratory Investigation:**
   - Under *Order Diagnostic Laboratory Test*, enter `Stat High-Sensitivity Troponin I & ECG` and click **Order Test**.
6. **Prescribe Medication:**
   - Under *Prescribe Medication*, enter `Aspirin 325mg chewable stat, Clopidogrel 300mg stat, Sorbitrate 5mg SL` and click **Record Prescription**.
7. **Allocate Hospital Bed:**
   - Select an available bed (e.g. `Bed 102`) and click **Assign Bed**.
8. Click **✓ Conclude Consultation**.
9. Click **Sign out**.

### Phase D: Laboratory Execution (Lab Tech)
1. Select **Laboratory** on the landing page (`/login/lab`).
2. Sign in with `lab@patientflow.local` / `Demo@1234`.
3. In the **Pending Orders** queue, locate Vikramaditya Rao's test:
   - Click **Enter Result**.
   - Input: `Troponin I: 0.28 ng/mL (Positive for acute myocardial injury). ST elevation in leads V1-V4.`
   - Click **Save & Complete Order**.
4. The test moves to the *Recent Completed Tests* audit table.
5. Click **Sign out**.

### Phase E: Medication Fulfillment (Pharmacy)
1. Select **Pharmacy** on the landing page (`/login/pharmacy`).
2. Sign in with `pharmacy@patientflow.local` / `Demo@1234`.
3. In the **Pending Orders** list, locate the prescription for Vikramaditya Rao.
4. Click **✓ Mark Dispensed**.
5. The order updates and moves to *Recent Dispensed Medications*.
6. Click **Sign out**.

### Phase F: Discharge Verification & Bed Release
1. Sign in as either `doctor1@patientflow.local` or `reception@patientflow.local`.
2. Click **Discharge Desk** (`/discharge`).
3. Notice Vikramaditya Rao is now listed under **Patients Eligible for Discharge** because all lab orders have been completed.
4. Click **Process Discharge**:
   - Enter Summary: `Acute coronary syndrome medically stabilized. Patient transferred to CCU telemetry.`
   - Click **Confirm Discharge & Release Bed**.
5. Open **Bed Capacity** (`/beds`):
   - Verify that `Bed 102` has immediately transitioned back from `OCCUPIED` to **AVAILABLE** (Green).

### Phase G: Administrative Simulation (Admin)
1. Select **Administrator** on the landing page (`/login/admin`).
2. Sign in with `admin@patientflow.local` / `Demo@1234`.
3. Review the hospital-wide KPI cards (Queue Count, Emergency Count, Bed Occupancy, Pending Labs & Prescriptions).
4. Click **Scheduler Simulation** (`/simulation`).
5. Click **Run simulation**.
6. Review the Recharts graph and table comparing **FCFS vs. Priority Scheduling**:
   - Demonstrates that Priority Scheduling drastically reduces wait times for Level 1 (Emergency) and Level 2 (Urgent) patients without starving lower triage levels due to linear time aging (`+1 pt/min`).
   - Click **Download results (CSV)**.

---

## 5. Security & Data Integrity Notes

- All endpoints are secured with **stateless JWT tokens** passed via the `Authorization: Bearer <token>` header.
- Endpoint authorization is strictly enforced in the Spring Boot backend with `@PreAuthorize("hasRole(...)")`.
- `DemoDataSeeder` uses idempotent checks to prevent duplicate records upon consecutive server restarts.
- The visual identity adheres to the **FlowAlloc: Quiet Precision** design system (Deep Navy `#142C40`, Muted Teal `#147D73`, Off-white `#F7F8F7`, and restrained status semantics).
