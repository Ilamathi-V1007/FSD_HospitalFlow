package com.hospitalflow.simulation;

import java.util.List;

/**
 * Response of POST /api/simulation/run. All waits are in minutes.
 * triageLevel 0 in {@link WaitStats} and {@link Comparison} means "all patients".
 */
public record SimulationResult(
        Parameters parameters,
        double offeredLoad,
        PolicyResult fcfs,
        PolicyResult priority,
        List<Comparison> comparison) {

    /** The parameters actually used (defaults filled in). */
    public record Parameters(int patients, int doctors, double arrivalsPerHour,
                             double avgConsultMinutes, int replications, long seed) {}

    public record WaitStats(int triageLevel, int count, double avgWait, double p90Wait, double maxWait) {}

    public record PolicyResult(String policy, List<WaitStats> byLevel, WaitStats overall) {}

    /** Positive = priority scheduler waits less than FCFS; negative = it waits more. */
    public record Comparison(int triageLevel, double avgImprovementPct, double p90ImprovementPct) {}
}
