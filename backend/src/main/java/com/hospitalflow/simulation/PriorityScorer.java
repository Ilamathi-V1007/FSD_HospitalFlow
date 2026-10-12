package com.hospitalflow.simulation;

/**
 * Charter v1 priority formula, as plain Java (no Spring, no database):
 *
 *   score = baseWeight[level] + agingFactor * waitMinutes
 *
 * Triage levels follow the charter: 1 = emergency, 2 = urgent, 3 = semi-urgent, 4 = non-urgent.
 * Base weights are 100 / 60 / 30 / 10 and agingFactor = 1, so a level-4 patient catches up with a
 * fresh level-3 patient after 20 minutes and with a fresh level-2 patient after 50 minutes.
 *
 * NOTE: the Queue module (Member 2) is expected to own the live-system scorer. This copy lives in
 * the simulation package so the simulator is self-contained. When the queue module exposes its own
 * scorer, replace the body of {@link #score} with a call to it so both run the identical algorithm.
 */
public final class PriorityScorer {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double AGING_FACTOR = 1.0;

    // index 0 unused so that BASE_WEIGHT[level] reads naturally
    private static final double[] BASE_WEIGHT = {0, 100, 60, 30, 10};

    private PriorityScorer() {}

    public static double baseWeight(int triageLevel) {
        requireValidLevel(triageLevel);
        return BASE_WEIGHT[triageLevel];
    }

    /** Higher score = served earlier. Negative waits are treated as zero. */
    public static double score(int triageLevel, double waitMinutes) {
        return baseWeight(triageLevel) + AGING_FACTOR * Math.max(0.0, waitMinutes);
    }

    static void requireValidLevel(int triageLevel) {
        if (triageLevel < MIN_LEVEL || triageLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Triage level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + " but was " + triageLevel);
        }
    }
}
