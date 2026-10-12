# Simulation module: FCFS vs. priority scheduler

Owner: Platform, Auth, Analytics and Simulation. Backend: `simulation/`. Frontend: `src/simulation/`.
Purpose: produce the evidence for charter objective 1 (priority scheduling vs. first-come-first-served).

## How it works

1. `ArrivalGenerator` creates seeded synthetic patients: Poisson arrivals, triage mix 5% / 15% / 35% / 45%
   (levels 1 to 4), consultation time = average x level factor (1.5 / 1.25 / 1.0 / 0.8) x a random 0.5 to 1.5.
2. `SimulationEngine` replays the same patients through N doctors (discrete-event, non-preemptive).
   Whenever a doctor is free, a `SchedulingPolicy` picks the next patient:
   - `FCFS`: earliest arrival.
   - `PRIORITY`: highest `PriorityScorer.score = baseWeight[level] + 1 x waitMinutes`
     (100 / 60 / 30 / 10), ties go to the earlier check-in.
3. `SimulationService` repeats this for `replications` seeds (seed, seed+1, ...), pools the waits and reports
   average, 90th percentile (nearest rank) and maximum wait per triage level and overall.

Same seed gives the same report. Both policies always see identical patients.

## API

`POST /api/simulation/run` (ADMIN only). Every body field is optional.

| Field | Default | Allowed |
|---|---|---|
| patients | 300 | 10 to 2000 |
| doctors | 3 | 1 to 20 |
| arrivalsPerHour | 14 | 1 to 500 |
| avgConsultMinutes | 12 | 3 to 60 |
| replications | 20 | 1 to 50 |
| seed | 42 | any integer |

Response: `parameters`, `offeredLoad`, `fcfs` and `priority` (each with `byLevel[1..4]` and `overall`),
and `comparison` (improvement % per level; `triageLevel` 0 = all patients).

## Reference result (defaults, seed 42, offered load 90%)

| Triage level | FCFS avg / P90 (min) | Priority avg / P90 (min) | Avg change |
|---|---|---|---|
| 1 Emergency | 13.6 / 34.7 | 2.5 / 5.8 | +81.7% |
| 2 Urgent | 14.4 / 34.4 | 4.4 / 9.8 | +69.4% |
| 3 Semi-urgent | 14.9 / 36.6 | 12.8 / 35.3 | +14.3% |
| 4 Non-urgent | 15.1 / 36.3 | 23.8 / 55.6 | -58.3% |
| All | 14.8 / 35.9 | 16.1 / 45.4 | -8.8% |

Reading it honestly: priority scheduling moves waiting from urgent patients to non-urgent ones. It does not
reduce the overall average wait. Aging keeps the non-urgent wait bounded. Report the per-level result.

## Assumptions and limitations

- Identical doctors, non-preemptive consultations, no lab or bed stages, no breaks.
- Triage mix and consultation times are assumed values, not measured. Tune them in `ArrivalGenerator`.
- `PriorityScorer` is a copy of the charter formula kept in this package. When the queue module exposes its own
  scorer, make `PriorityScorer.score` call it so the live system and simulator share one implementation.
- Triage levels are 1 to 4 (charter). The current `queue_entries.urgency_level` column allows 1 to 5 with 5 as
  critical; Intake and Queue needs to align that before the live scorer is shared.
