# FlowAlloc — System Architecture & Workflow Specifications

> **System Name:** FlowAlloc — Dynamic Patient Flow and Resource Optimization System  
> **Tagline:** “Right patient, right resource, right time.”  
> **Documentation Type:** Visual Architecture, Component Breakdown & Data Flow Specifications

---

## 1. System Architecture Diagram

```mermaid
flowchart TD
    %% CLIENT LAYER
    subgraph ClientLayer["🖥️ Frontend Client Layer (React 19 + Vite 6)"]
        Browser["Clinician Web Browser<br/>(Port 5173 / Vercel)"]
        
        subgraph AuthState["Client State & Routing"]
            AuthCtx["AuthContext<br/>(LocalStorage JWT Bearer Token)"]
            RoleRoute["ProtectedRoute Guard<br/>(Role-Based Access Control)"]
        end

        subgraph Workspaces["Role-Specific Clinical Desks"]
            AdminUI["Admin Dashboard & Staff Manager<br/>(/admin)"]
            ReceptionUI["Receptionist Intake & Triage<br/>(/receptionist)"]
            QueueUI["Live OPD Ward Board<br/>(/queue)"]
            DoctorUI["Doctor Consultation Desk<br/>(/doctor)"]
            BedUI["Live Bed Inventory Board<br/>(/beds)"]
            LabUI["Laboratory Worklist<br/>(/lab)"]
            PharmUI["Pharmacy Dispensing Desk<br/>(/pharmacy)"]
            DischargeUI["Discharge Ledger & Bed Clearance<br/>(/discharge)"]
            SimUI["Discrete-Event Capacity Simulation<br/>(/simulation)"]
        end

        Browser --> AuthCtx
        AuthCtx --> RoleRoute
        RoleRoute --> Workspaces
    end

    %% API GATEWAY & SECURITY LAYER
    subgraph APILayer["🛡️ Backend Security & REST API Layer (Spring Boot 3.3.4 - Port 8082)"]
        JwtFilter["JwtAuthFilter<br/>(HMAC-SHA256 Token Validator)"]
        SecConfig["SecurityConfig & CORS<br/>(Stateless Session, BCrypt, RBAC)"]

        subgraph Controllers["Spring REST Controllers"]
            AuthCtrl["AuthController<br/>(/api/auth/*)"]
            QueueCtrl["QueueController<br/>(/api/queue/*)"]
            AllocCtrl["AllocationController<br/>(/api/allocation/*)"]
            LabPharmCtrl["LabPharmacyController<br/>(/api/labpharmacy/*)"]
            SimCtrl["SimulationController<br/>(/api/simulation/*)"]
            HealthCtrl["HealthController<br/>(/health)"]
        end

        JwtFilter --> SecConfig
        SecConfig --> Controllers
    end

    %% SERVICE & ALGORITHM LAYER
    subgraph ServiceLayer["⚙️ Domain Services & Algorithm Engines"]
        AuthSvc["AuthService<br/>(User Management & Doctor Provisioning)"]
        QueueSvc["QueueService<br/>(Patient Intake & Triage Mapping)"]
        PriorityEngine["PriorityScorer Algorithm<br/>Score = Base Urgency + Wait Time Minutes<br/>(Anti-Starvation Aging)"]
        AllocSvc["AllocationService<br/>(Doctor State Machine & Bed Reservation)"]
        LabPharmSvc["LabPharmacyService<br/>(Diagnostic Orders, Rx & Discharge)"]
        SimEngine["SimulationEngine<br/>(Poisson Arrivals, Exponential Service,<br/>FCFS vs. Priority Benchmark)"]

        AuthCtrl --> AuthSvc
        QueueCtrl --> QueueSvc
        QueueSvc --> PriorityEngine
        AllocCtrl --> AllocSvc
        LabPharmCtrl --> LabPharmSvc
        SimCtrl --> SimEngine
    end

    %% DATA PERSISTENCE LAYER
    subgraph DataLayer["🗄️ Persistence Layer (PostgreSQL 16 - Port 5432)"]
        subgraph Entities["Spring Data JPA Repositories & Relational Tables"]
            TblUsers[("users<br/>(Credentials, Roles, BCrypt)")]
            TblDoctors[("doctors<br/>(Status: AVAILABLE, BUSY, OFFLINE)")]
            TblPatients[("patients<br/>(Demographics & Medical Record)")]
            TblQueues[("queue_entries<br/>(Triage L1-L4, Vitals, Live Priority Score)")]
            TblAppts[("appointments<br/>(Status: IN_PROGRESS, COMPLETED)")]
            TblBeds[("beds<br/>(ICU, GENERAL, EMERGENCY, STEP_DOWN)")]
            TblLabRx[("lab_requests & prescriptions<br/>(Diagnostic Status & Dispensation)")]
            TblSummary[("visit_summaries<br/>(Clinical Diagnoses & Notes)")]
        end

        AuthSvc --> TblUsers
        AuthSvc --> TblDoctors
        QueueSvc --> TblPatients
        QueueSvc --> TblQueues
        AllocSvc --> TblAppts
        AllocSvc --> TblBeds
        AllocSvc --> TblSummary
        LabPharmSvc --> TblLabRx
        LabPharmSvc --> TblBeds
    end

    %% CROSS-LAYER COMMUNICATION
    Workspaces -- "HTTPS / JSON REST Calls + Authorization: Bearer <JWT>" --> JwtFilter
```

---

## 2. Layer-by-Layer Architectural Breakdown

### 2.1 Frontend Client Layer (React 19 + Vite 6)
- **Design Philosophy:** "The OPD Ward Board" — high contrast paper-register styling with monospaced clinical indicators and tabbed navigation.
- **Role Routing:** Managed through `ProtectedRoute.jsx` using React Router v7. Unauthenticated users are redirected to `/login`, while unauthorized roles are bounced back to their respective clinical dashboard.
- **State Management:** React Context API (`AuthContext.jsx`) stores the active user details, roles, and session token in `localStorage`.
- **API Interceptor:** Axios (`api.js`) automatically appends `Authorization: Bearer <token>` to all HTTP requests.

### 2.2 Security & REST API Layer (Spring Boot 3.3.4)
- **Token Filter:** `JwtAuthFilter` extracts Bearer tokens on every incoming request, validates cryptographic HMAC-SHA256 signatures, and injects user permissions into the `SecurityContext`.
- **Role-Based Access Control (RBAC):** Configured in `SecurityConfig.java`:
  - Public endpoints: `/api/auth/login`, `/api/auth/register`, `/health`
  - Admin-only endpoints: `/api/auth/staff/**`
  - Doctor endpoints: `/api/allocation/call-next`, `/api/allocation/appointments/*/complete`
  - Role-specific endpoints for Receptionists, Lab Technicians, and Pharmacists.

### 2.3 Business Logic & Algorithms
- **Dynamic Priority Scoring (`PriorityScorer.java`):**
  $$\text{Priority Score} = \text{Base Acuity Weight}(\text{Triage Level}) + \text{Wait Time in Minutes}$$
  - Level 1 (Resuscitation): Base 100
  - Level 2 (Emergency): Base 75
  - Level 3 (Urgent): Base 50
  - Level 4 (Routine): Base 25
  - Aging rate of $+1.0$ point per elapsed minute guarantees **anti-starvation** for low-urgency patients.
- **Doctor State Machine (`AllocationController.java`):** Enforces atomic transitions between `AVAILABLE` and `BUSY`, ensuring no doctor claims multiple patients simultaneously.
- **Discrete-Event Simulation (`SimulationEngine.java`):** Models Poisson synthetic arrivals to empirically compare 90th percentile wait times between FCFS and Priority queueing policies.

### 2.4 Data Persistence Layer (PostgreSQL 16)
- Relational schema managed by Spring Data JPA with `hibernate.ddl-auto: update`.
- Normalized relational tables mapping patient intake, clinical queues, doctor assignments, hospital bed reservations, diagnostic laboratory orders, and pharmaceutical prescriptions.

---

## 3. End-to-End Clinical Data Flow

```mermaid
sequenceDiagram
    autonumber
    actor Rec as Receptionist
    actor Doc as Attending Doctor
    actor Lab as Lab Technician
    actor Pharm as Pharmacist
    actor Admin as Ward Admin
    participant API as Spring Boot API
    participant DB as PostgreSQL DB

    Note over Rec, DB: Phase 1: Intake & Triage
    Rec->>API: POST /api/queue/checkin (Name, Vitals, Triage Level)
    API->>DB: Save Patient & QueueEntry (Status: WAITING, Priority: 75)
    DB-->>API: Saved
    API-->>Rec: Check-In Ticket Generated

    Note over Doc, DB: Phase 2: Consultation & Bed Reservation
    Doc->>API: POST /api/allocation/call-next
    API->>DB: Lock Doctor to BUSY, Create Appointment (IN_PROGRESS)
    API-->>Doc: Patient Data & Vitals Loaded
    Doc->>API: POST /api/allocation/appointments/{id}/assign-bed (Bed: ICU-101)
    API->>DB: Update Bed ICU-101 (Status: OCCUPIED)
    Doc->>API: POST /api/allocation/appointments/{id}/complete (Notes, Lab, Rx)
    API->>DB: Save VisitSummary, Create LabRequest & Prescription, Release Doctor to AVAILABLE
    API-->>Doc: Consultation Completed

    Note over Lab, DB: Phase 3: Diagnostic Fulfillment
    Lab->>API: POST /api/labpharmacy/lab-requests/completed
    API->>DB: Update LabRequest (Status: COMPLETED)

    Note over Pharm, DB: Phase 4: Pharmacy Dispensing
    Pharm->>API: POST /api/labpharmacy/prescriptions/dispensed
    API->>DB: Update Prescription (Status: DISPENSED)

    Note over Admin, DB: Phase 5: Discharge & Capacity Recovery
    Admin->>API: POST /api/labpharmacy/discharge/{id}
    API->>DB: Finalize Visit, Reset Bed ICU-101 (Status: AVAILABLE)
    API-->>Admin: Discharge Confirmed & Bed Freed
```

---

## 4. Port & Network Topology

| Component | Local Dev Port | Production Host | Protocol |
| :--- | :--- | :--- | :--- |
| **Frontend SPA** | `5173` | Vercel | HTTPS / Web |
| **Backend REST API** | `8082` | Render (Docker) | HTTPS / JSON |
| **PostgreSQL Database** | `5432` | Render Managed PG | PostgreSQL TCP |
| **Health Probe** | `8082/health` | Render Health Check | HTTP GET |
