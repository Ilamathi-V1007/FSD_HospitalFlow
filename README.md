# Patient Flow Coordination System (OPD)

A role-secured web system that moves outpatients from check-in to discharge. Instead of first-come-first-served, waiting patients are ranked by **triage urgency plus time waited**, doctors call the next patient from that ranking, and beds and lab requests are coordinated transactionally so resources are never double-booked. Staff see queues, doctors and beds update live.

Full requirements, ownership rules and workflow are in [`docs/`](docs/) (see the project charter).

## Run it

Requires Docker.

```bash
cp .env.example .env     # optional; defaults work for a demo
docker compose up --build
```

| What | Where |
|---|---|
| App | http://localhost:3000 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |

Demo accounts are created on first start (password for all: `Demo@1234`):

| Role | Email |
|---|---|
| Administrator | admin@patientflow.local |
| Receptionist | reception@patientflow.local |
| Doctor | doctor1@patientflow.local, doctor2@patientflow.local |
| Lab technician | lab@patientflow.local |
| Pharmacist | pharmacy@patientflow.local |

Set `SEED_DEMO_DATA=false` (and a real `JWT_SECRET`) outside demos.

### Local development

```bash
# Database
docker compose up db -d
# Backend (needs Java 21 and Maven); API on :8080
cd backend && mvn spring-boot:run
# Frontend (needs Node 20); dev server on :5173 and proxies /api and /ws to :8080
cd frontend && npm install && npm run dev
```

Backend tests use Testcontainers, so Docker must be running: `cd backend && mvn verify`.

## Demo script (about 5 minutes)

Use two browsers or windows so you can see live updates.

1. Sign in as **Receptionist**. Register two patients and check them in: one at triage 4 with "mild cough", one at triage 1 with "chest pain". Open **Queue**: the emergency is first even though they arrived second.
2. Sign in as **Doctor** (second window). Set status to Available and **Call next patient**: the emergency patient is called. The queue in the first window updates without a refresh.
3. As the doctor, **Request lab test**, **Add prescription**, **Assign bed**. Try completing the consultation: it works, but discharge will be blocked until the lab result is in.
4. Sign in as **Lab technician** and **Enter result**. The doctor and discharge screens update.
5. As the doctor or receptionist, open **Discharge**, review, and discharge the patient. The bed is freed and the visit is closed.
6. Sign in as **Administrator** to see overview numbers and manage staff accounts. Open **Beds** and try assigning the same bed from two windows at once: only one succeeds.

## How it works

**Priority score:** `score = baseWeight[triage level] + 1 point per minute waited`, with base weights 100 / 60 / 30 / 10 for levels 1 to 4 (emergency, urgent, semi-urgent, non-urgent). A non-urgent patient overtakes a fresh semi-urgent one after 20 minutes, a fresh urgent one after 50 and a fresh emergency after 90, so nobody starves. Ties go to the earlier check-in. The engine is plain Java in `backend/.../queue/engine/PriorityScorer.java` with no Spring or database dependency, so the v1.1 simulator can reuse it unchanged.

**Estimated wait:** patients ahead ÷ doctors available × average consultation time.

**Conflict prevention:** bed assignment locks the bed row (`SELECT ... FOR UPDATE`), and partial unique indexes in the database refuse a second active assignment as a backstop. `call-next` locks the doctor row and claims the top-ranked waiting visit atomically.

**Discharge** is one transaction across modules: write the summary, release the bed, close the visit.

**Live updates:** the server publishes small "something changed" events over WebSocket (STOMP). They carry no patient data; clients refetch through the authenticated REST API.

## Project layout and ownership

```
backend/src/main/java/com/patientflow/
  auth/         users, login, JWT, security config         (Platform, Auth, Analytics, Simulation owner)
  queue/        patients, visits, queue, scoring engine    (Intake and Queue owner)
  allocation/   doctors, consultations, beds               (Allocation owner)
  labpharmacy/  lab requests, prescriptions, discharge     (Lab, Pharmacy, Discharge owner)
  common/       roles, errors, live updates, seeding       (shared, change by agreement)
backend/src/main/resources/db/migration/   Flyway migrations (V1 schema, V2 beds)
frontend/src/{auth,queue,allocation,labpharmacy,common}/   matching UI folders
```

Rules: cross-module calls go through public service methods, never another module's tables. Schema changes are new Flyway migration files. Roles and endpoint access are enforced with `@PreAuthorize`; see Swagger UI for every endpoint.

## Contributing (team workflow)

The v1.0 baseline (skeleton plus all v1 modules) was generated with AI by the lead, verified locally, and pushed as the first commits. From here:

1. Clone, run `docker compose up --build`, and read your own module.
2. Pick an issue labelled with your module (backlog in [`docs/V1.1_ISSUES.md`](docs/V1.1_ISSUES.md)).
3. Branch from `dev`: `feature/<module>-<short-desc>`.
4. Commit with a module prefix (`queue: add aging tests`), push, and open a PR into `dev`. CI must pass; the lead reviews.

Nobody commits directly to `main` or `dev`.

## Known limitations (v1.0)

- Profile page, analytics and the FCFS-vs-priority simulation are not built yet (v1.1). The pharmacist page polls every 15 seconds because the server publishes no live event for prescriptions.
- Demo accounts and a default JWT secret exist for convenience; change both before any real deployment.
- WebSocket origins are open and the STOMP connection is not authenticated (events contain no patient data).
- Beds are not tied to wards beyond a label; there is no inpatient management after OPD discharge.
- Patient data is synthetic. Do not enter real patient information.

## AI-use statement

The v1.0 skeleton and baseline modules were generated with Claude (Anthropic), then run, tested and reviewed by the team. Course policy on AI-assisted work should be confirmed and this section updated if disclosure is required.
