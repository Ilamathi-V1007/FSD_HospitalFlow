package com.hospitalflow.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeded synthetic patient arrivals. The same seed always gives the same patients, which is what lets
 * FCFS and the priority scheduler be compared on identical input.
 *
 * Model: Poisson arrivals (exponential gaps), a fixed triage mix, and consultation times that are
 * longer for sicker patients. All of this is synthetic, never real patient data.
 */
public final class ArrivalGenerator {

    /** Share of arrivals at triage level 1..4 (index 0 unused). Sums to 1.0. */
    static final double[] TRIAGE_MIX = {0, 0.05, 0.15, 0.35, 0.45};

    /** Consultation length relative to the average, per triage level (index 0 unused). */
    static final double[] SERVICE_FACTOR = {0, 1.5, 1.25, 1.0, 0.8};

    static final double MIN_SERVICE_MINUTES = 2.0;

    private ArrivalGenerator() {}

    public static List<SimPatient> generate(int count, double arrivalsPerHour, double avgConsultMinutes, long seed) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        if (arrivalsPerHour <= 0 || avgConsultMinutes <= 0) {
            throw new IllegalArgumentException("arrivalsPerHour and avgConsultMinutes must be > 0");
        }

        Random rng = new Random(seed);
        double meanGapMinutes = 60.0 / arrivalsPerHour;
        List<SimPatient> patients = new ArrayList<>(count);

        double clock = 0.0;
        for (int id = 1; id <= count; id++) {
            clock += -Math.log(1.0 - rng.nextDouble()) * meanGapMinutes; // exponential gap
            int level = drawLevel(rng.nextDouble());
            double service = Math.max(MIN_SERVICE_MINUTES,
                    avgConsultMinutes * SERVICE_FACTOR[level] * (0.5 + rng.nextDouble())); // x0.5 .. x1.5
            patients.add(new SimPatient(id, level, clock, service));
        }
        return patients;
    }

    private static int drawLevel(double u) {
        double cumulative = 0.0;
        for (int level = PriorityScorer.MIN_LEVEL; level <= PriorityScorer.MAX_LEVEL; level++) {
            cumulative += TRIAGE_MIX[level];
            if (u < cumulative) {
                return level;
            }
        }
        return PriorityScorer.MAX_LEVEL;
    }
}
