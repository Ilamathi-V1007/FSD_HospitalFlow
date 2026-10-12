package com.hospitalflow.simulation;

import java.util.List;

/** How a free doctor chooses the next patient from the waiting room. */
public enum SchedulingPolicy {

    /** First come, first served: earliest arrival wins. */
    FCFS {
        @Override
        public SimPatient pick(List<SimPatient> waiting, double nowMinute) {
            SimPatient best = null;
            for (SimPatient p : waiting) {
                if (best == null || earlierArrival(p, best)) {
                    best = p;
                }
            }
            return best;
        }
    },

    /** Charter priority scheduler: highest {@link PriorityScorer} score wins, ties go to earlier check-in. */
    PRIORITY {
        @Override
        public SimPatient pick(List<SimPatient> waiting, double nowMinute) {
            SimPatient best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (SimPatient p : waiting) {
                double score = PriorityScorer.score(p.triageLevel(), nowMinute - p.arrivalMinute());
                if (best == null
                        || score > bestScore
                        || (score == bestScore && earlierArrival(p, best))) {
                    best = p;
                    bestScore = score;
                }
            }
            return best;
        }
    };

    /** Returns the patient to serve next, or null if nobody is waiting. */
    public abstract SimPatient pick(List<SimPatient> waiting, double nowMinute);

    private static boolean earlierArrival(SimPatient a, SimPatient b) {
        if (a.arrivalMinute() != b.arrivalMinute()) {
            return a.arrivalMinute() < b.arrivalMinute();
        }
        return a.id() < b.id();
    }
}
