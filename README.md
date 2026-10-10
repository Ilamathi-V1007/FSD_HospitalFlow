# Patient Flow Coordination System

An OPD patient flow and resource coordination platform. See `Project_Charter_Patient_Flow_System.md` (project docs) for full objectives, module ownership, and page-by-page functionality.

## Structure

```
hospitalflow/
├── db/schema.sql          # Locked Week-1 schema (source of truth)
├── backend/                # Spring Boot (Java 17) — modules: auth, queue, allocation, labpharmacy, common
├── frontend/                # React (Vite) — modules: auth, queue, allocation, labpharmacy, common
└── docker-compose.yml       # Runs Postgres + backend + frontend together
```

## Running locally (without Docker)

**1. Database**
```bash
# Create a local Postgres DB named "hospitalflow", then:
psql -U postgres -d hospitalflow -f db/schema.sql
```

**2. Backend**
```bash
cd backend
export DB_USER=postgres DB_PASSWORD=postgres JWT_SECRET=dev-secret-change-me
mvn spring-boot:run
# Runs on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health check: http://localhost:8080/health
```

**3. Frontend**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

## Running with Docker (all-in-one)

```bash
docker compose up --build
```

## First login

There's no seeded user yet. Create the first Admin account via:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@hospital.com","password":"admin123","role":"ADMIN"}'
```
Then log in from the frontend with those credentials.

## Module ownership (see charter for full detail)

| Module | Backend package | Frontend folder | Tables |
|---|---|---|---|
| Auth & Users | `auth/` | `src/auth/` | `users` |
| Patient Intake & Queue | `queue/` | `src/queue/` | `patients`, `queue_entries` |
| Doctor & Resource Allocation | `allocation/` | `src/allocation/` | `doctors`, `beds`, `appointments` |
| Lab, Pharmacy & Discharge | `labpharmacy/` | `src/labpharmacy/` | `lab_requests`, `prescriptions`, `visit_summaries` |
| Shared | `common/` | `src/common/` | — |

**Rule:** cross-module calls go through service methods, never direct cross-module DB/entity access.

## Week 1 status

- [x] Schema locked (`db/schema.sql`)
- [x] Backend skeleton: package structure, working JWT auth (register/login), Spring Security config, WebSocket config, global exception handler, health check
- [x] Frontend skeleton: routing to all 11 role-based pages, protected routes, shared API client with JWT injection
- [x] Docker Compose for one-command local run
- [x] Verified `docker compose up --build` and full register/login flow against Postgres — working
