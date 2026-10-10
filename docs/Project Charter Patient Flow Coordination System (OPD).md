# Project Charter: Patient Flow Coordination System (OPD)

**Course:** Full Stack Development  |  **Team size:** 4  |  **Stack:** Spring Boot + PostgreSQL + React

## 1. Overview

**Problem.** Outpatient departments (OPDs) face unpredictable patient inflow. Patients are usually served first-come-first-served regardless of medical urgency, doctor workload is uneven, and staff have no real-time view of queues, doctor availability, beds, or lab backlogs. The result is long, unstructured waits and avoidable resource conflicts.

**Solution.** A web-based, role-secured system that digitizes check-in, prioritizes waiting patients by urgency and waiting time, coordinates doctors, beds and lab requests, and gives staff live visibility of the whole patient journey, from check-in to discharge.

**Scope statement.** The system covers one OPD visit end to end: registration, triage, queue, consultation, optional lab tests, optional admission to a bed, prescription, and discharge. Bed allocation is included as *admission from OPD*, and it is also the main demonstration of transactional conflict prevention.

**What this is not:** a general Hospital Management System. The backend makes decisions (prioritization, allocation, conflict prevention) rather than only storing records.

## 2. Objectives and Success Measures

| # | Objective | How it is verified |
| --- | --- | --- |
| 1 | Reduce waiting time compared with FCFS | Simulation report: same synthetic arrivals run through FCFS and through our priority scheduler; compare average and 90th-percentile wait per urgency level |
| 2 | Real-time staff visibility | Live queue and bed pages update via WebSocket with no manual refresh (demonstrated in two browsers) |
| 3 | No resource conflicts | Concurrency test: parallel requests for the same bed result in exactly one success |
| 4 | Secure, role-based access | Each role can reach only its own endpoints; wrong role returns 403 (automated test per module) |
| 5 | Technical depth beyond CRUD | Scheduling engine with unit tests, transactional allocation, WebSocket updates, OpenAPI documentation |
| 6 | Easy to run and grade | `docker compose up` starts backend, database and frontend with seeded demo users for every role |

## 3. Why a Team of Four

After the v1.0 baseline is pushed, the project continues as four parallel workstreams. Each has its own code, data, tests and pull requests, so each member has a distinct, traceable contribution:

1. **Platform and security (Member 1):** authentication hardening, admin tools, CI and Docker, plus the v1.1 analytics dashboard and the FCFS-vs-priority simulation that proves the system works.
2. **Intake and prioritization (Member 2):** patient registration, visits, the live queue, the scoring engine, and WebSocket updates; tuning and tests.
3. **Doctor and resource allocation (Member 3):** doctor availability, consultations, and transactional bed assignment; concurrency tests.
4. **Clinical downstream (Member 4):** lab requests, the v1.1 pharmacist page, prescriptions, discharge, and the visit summary.

The v1.0 baseline itself is generated with AI by the lead (Section 9). The four workstreams above describe the work that follows it: fixing and hardening, v1.1 features, tests, and documentation. Contributions are tracked through module ownership and each member's issues and PRs in GitHub.

## 4. Scope

**Version 1 (core vertical slice):**

- Login with role-based redirect
- Receptionist: register or search patient, enter symptoms and triage level, check in
- Live priority queue
- Doctor: call next, view patient context, complete consultation, request lab test, write prescription, assign bed, set own availability
- Bed status with transactional assignment and release
- Lab technician: view requests, enter results, mark complete
- Discharge and visit summary (releases bed, closes visit)
- Admin: create, edit and deactivate staff accounts

**Version 1.1 (PR work after v1 is pushed):**

- Simulation module (FCFS vs. priority scheduler) and its report
- Analytics dashboard (peak hours, average wait trend, bottleneck by stage, doctor workload)
- Pharmacist dashboard (view and dispense prescriptions)
- Profile page (view and edit own details, change password)
- Bug fixes, edge cases, UI polish, extra tests

**Out of scope:** billing, insurance, appointments booked in advance, inpatient ward management, real patient data, microservices, ML models. Optional later extension: wait-time prediction from historical data.

## 5. Final Tech Stack

| Layer | Choice | Notes |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 3 | REST APIs, dependency injection |
| Security | Spring Security + JWT, BCrypt | Stateless auth, role-based access |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL | Row-level locking for allocation logic |
| Migrations | Flyway | Versioned schema, replaces ad hoc `schema.sql`; JPA `ddl-auto` set to `validate` |
| Validation | `spring-boot-starter-validation` | `@Valid` on all request DTOs |
| Real-time | Spring WebSocket (STOMP) | Live queue and bed updates |
| API docs | springdoc-openapi (Swagger UI) | Contract between frontend and backend |
| Testing | JUnit 5, Mockito, Testcontainers | Real PostgreSQL in integration tests |
| Frontend | React + TypeScript + Vite | TypeScript is the decision, not optional |
| UI and data | Tailwind CSS, shadcn/ui, TanStack Query, Recharts | TanStack Query for server state |
| Containers | Docker + Docker Compose | Set up in Phase 0, not at the end |
| CI | GitHub Actions (required) | Build and test on every PR |
| Config | `application-{profile}.yml` + `.env` | No hardcoded DB credentials or JWT secret |
| Logging | SLF4J + Logback | Log login, allocation, lab and discharge events |

Redis is not used in v1.

## 6. Domain Model and Core Logic

### 6.1 Entities and owners

| Table | Purpose | Owner module |
| --- | --- | --- |
| `users` | Staff accounts and roles | Auth |
| `patients` | Registered patients | Queue |
| `visits` | **Central entity**: one row per OPD visit (patient, check-in time, triage level, status, assigned doctor, discharge time). Everything else references it. | Queue |
| `queue_entries` | Position and priority of a waiting visit | Queue |
| `doctors` | Doctor profile and availability status | Allocation |
| `consultations` | A doctor seeing a visit (start and end time, notes) | Allocation |
| `beds`, `bed_assignments` | Bed inventory and occupancy history | Allocation |
| `lab_requests` | Tests requested and results | Lab and Discharge |
| `prescriptions` | Medicines prescribed | Lab and Discharge |
| `visit_summaries` | Final summary at discharge | Lab and Discharge |

Visit lifecycle: `CHECKED_IN → WAITING → IN_CONSULTATION → (LAB_PENDING) → (ADMITTED) → DISCHARGED`.

### 6.2 Priority scoring (v1 formula)

- Triage levels: 1 = emergency, 2 = urgent, 3 = semi-urgent, 4 = non-urgent.
- `score = baseWeight[level] + agingFactor × waitMinutes`, with base weights 100 / 60 / 30 / 10 and `agingFactor = 1`.
- Aging guarantees no patient starves: a level-4 patient overtakes a fresh level-3 patient after 20 minutes of waiting, and a fresh level-2 patient after 50.
- Ties are broken by earlier check-in time.
- **Estimated wait** = patients ahead ÷ available doctors × rolling average consultation duration.
- The scoring engine is plain Java with no Spring or database dependencies, so the live system and the simulator run the identical algorithm.

### 6.3 Allocation and transactions

- Bed assignment uses a transaction with a row lock (`SELECT ... FOR UPDATE`) so two requests can never take the same bed.
- Discharge is one transaction that closes the visit, releases the bed and removes any queue entry. The discharge service (Lab and Discharge module) orchestrates it by calling public service methods of the other modules.
- Use synthetic patient data only. Passwords are BCrypt-hashed and secrets come from environment variables.

## 7. Module Ownership (Rebalanced)

Each module is a vertical slice: its own database tables, backend package and frontend folder.

Owners take responsibility for their module from the `v1.0` tag onward: fixing issues, adding v1.1 features, writing tests, and reviewing changes to it. The v1.0 baseline itself is generated by the lead (Section 9).

| Module | Owner | Backend package | Frontend folder | Pages |
| --- | --- | --- | --- | --- |
| Platform, Auth, Analytics and Simulation | Member 1 | `auth/`, `analytics/`, `simulation/` | `src/auth/`, `src/analytics/` | Login, Admin, Profile, Analytics. Also owns Docker, CI, Flyway baseline, `SecurityConfig` |
| Patient Intake and Queue | Member 2 | `queue/` | `src/queue/` | Receptionist, Live Queue. Owns the scoring engine and `WebSocketConfig` |
| Doctor and Resource Allocation | Member 3 | `allocation/` | `src/allocation/` | Doctor Dashboard, Bed Status |
| Lab, Pharmacy and Discharge | Member 4 | `labpharmacy/` | `src/labpharmacy/` | Lab Technician, Pharmacist, Discharge |
| Shared | All, by agreement | `common/` | `src/common/` | Layout, routing, API client, auth context |

**Rule:** if your work needs a file outside your package, either ask the owner to expose a service method, or flag it in the team chat and get sign-off first.

## 8. Shared Files Protocol

| File or area | Rule |
| --- | --- |
| `SecurityConfig.java` | Member 1 edits; others request changes |
| `WebSocketConfig.java` | Member 2 edits; Member 3 requests changes for bed updates |
| `GlobalExceptionHandler.java` | Shared, append-only: add your own exception blocks without touching others |
| Flyway migrations | Baseline `V1` is created in Phase 0. After that, each change is a new numbered migration file (never edit an applied one); announce it in the chat |
| Cross-module foreign keys | Fixed in Phase 0 (for example `queue_entries.visit_id`); not renamed later without team agreement |
| `application.yml`, `pom.xml` | Only add; announce new dependencies before adding |
| OpenAPI contract | Changes to another module's endpoints go through that module's owner |

**Golden rule:** cross-module communication goes through public service methods, never direct access to another module's entities or tables.

## 9. Git Workflow and Phases

**Who does what.** The *lead* (repository owner) generates the v1 baseline with Claude (AI), verifies it locally, pushes it, and reviews and merges every later PR. The other three members own modules from the `v1.0` tag onward.

**Branches.** `main` (stable, tagged releases) → `dev` (integration) → `feature/<module>-<short-desc>`. After the first push, nobody commits directly to `main` or `dev`.

**Phase 1: Baseline (lead only, before anyone else commits).** The skeleton and the complete v1 are built with AI by the lead and pushed together as the **first commits** to the repository. The baseline contains:

- **Skeleton:** Spring Boot and React projects, Docker Compose, CI workflow, Flyway `V1` migration for all tables, OpenAPI contract, auth setup, seed data with a demo user per role
- **v1 modules:** every v1 page and endpoint in Section 4, each in its own package and folder as laid out in Section 7
- **Documentation:** a README (setup, module guide, API docs link, known limitations), this charter in `/docs`, and an AI-use statement

Before pushing, the lead confirms that `docker compose up` works on a clean clone, the full demo flow runs, and the tests pass. The push is split into readable commits (for example `chore: project skeleton`, then one commit per module such as `queue: v1 baseline`). Then the lead tags `v1.0`, creates `dev` from `main`, and turns on branch protection and required CI.

**Phase 2: Team work through PRs.** The other members clone the repository and run `docker compose up`. Each owner reads their module and gives the team a short walkthrough. The lead logs GitHub Issues, labeled by module: bugs found in the baseline, tests to add, and the v1.1 features (simulation, analytics, pharmacist page, profile page). Each member then:

1. Takes issues labeled with their own module
2. Branches from `dev` as `feature/<module>-<short-desc>`
3. Fixes or builds it, then raises a PR into `dev` for the lead to review

**PR rules:**

- One issue or feature per PR, with a short description of what changed and how it was tested
- CI must pass; the lead and at least one other member review before merge
- Commit messages are prefixed with the module, for example `queue: add aging to priority score`
- Reviewers check that nothing outside the author's package was changed
- `dev` is merged into `main` at agreed milestones and tagged (`v1.1`, and so on)

## 10. Working With AI

- The v1.0 baseline and skeleton are generated with Claude (AI) by the lead, and this is disclosed in the README and the report. Confirm the course policy on AI-assisted work and adjust the disclosure if required.
- The baseline is pushed only after it builds, runs with `docker compose up`, and passes its tests. Known gaps are listed in the README.
- Every member must be able to explain and demo their own module, including its core logic. Each owner gives a short module walkthrough after v1.0.
- For later changes, give the AI the charter, the OpenAPI contract and your module boundaries as context, and instruct it not to edit files outside your package.
- AI-assisted code in a PR is reviewed like any other: the author has run it, tested it, and can explain it.

## 11. Definition of Done

**Per module, applying to every PR after v1.0:**

- [ ] All v1 pages for the module work against the real backend
- [ ] Role-based access verified (wrong role returns 403)
- [ ] Request DTOs validated and endpoints documented in OpenAPI
- [ ] Core logic has at least one JUnit test (scoring, bed locking, discharge transaction)
- [ ] No cross-module table access, only service calls
- [ ] Merged into `dev` through a reviewed PR with CI passing

**For v1.0:**

- [ ] `docker compose up` works on a clean clone, with seeded demo users for every role
- [ ] Full flow works: check-in → queue → consultation → lab → bed → discharge
- [ ] Live queue updates visible in two browsers at once
- [ ] Swagger UI loads and matches the implemented endpoints
- [ ] README covers setup, module guide, known limitations and the AI-use statement
- [ ] A written demo script exists for evaluation
- [ ] Pushed as the first commits to `main` and tagged `v1.0`; `dev` created; branch protection and CI enabled

## 12. Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| The AI-generated baseline does not build or run cleanly | The lead runs `docker compose up` and the full demo flow on a clean clone before the first push; known gaps are listed in the README |
| One large first push is hard to review | Commits split by layer and module; README module guide; each owner walks the team through their module |
| Members cannot explain code they did not write | Module ownership, short walkthroughs after v1.0, and review comments required on every PR |
| Uneven contribution visible in Git history | The baseline is single-author by design; from v1.0 onward, issues are labeled by module and shared evenly so each member has their own PRs |
| Merge conflicts despite the folder split | Ownership rules, `common/` changes by agreement, Flyway numbered migrations |
| Concurrency bugs in bed or queue logic | Dedicated concurrency tests; row locking verified before and after changes |
| WebSocket issues found late | A live-update test is part of the baseline and stays in CI |

## 13. Open Items to Confirm

- Course policy on AI-assisted development (including an AI-built baseline) and on a team size of four; get the justification in Section 3 approved if required
- Names of the four members against the module owners in Section 7, and who the lead (repository owner) is
- Repository host and visibility (private GitHub repo; branch protection on `main` and `dev` after the first push)
