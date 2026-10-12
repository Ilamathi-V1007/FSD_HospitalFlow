package com.hospitalflow.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Discrete-event, non-preemptive simulation of one OPD with N identical doctors.
 * Whenever a doctor becomes free, the {@link SchedulingPolicy} picks who is seen next.
 * Plain Java: no Spring, no database, no randomness (randomness lives in {@link ArrivalGenerator}).
 */
public final class SimulationEngine {

    /** Result for one patient: how long they waited before a doctor started seeing them. */
    public record PatientOutcome(SimPatient patient, double waitMinutes) {}

    private SimulationEngine() {}

    public static List<PatientOutcome> run(List<SimPatient> arrivals, int doctors, SchedulingPolicy policy) {
        if (doctors < 1) {
            throw new IllegalArgumentException("Need at least one doctor");
        }
        if (arrivals.isEmpty()) {
            return List.of();
        }

        List<SimPatient> incoming = new ArrayList<>(arrivals);
        incoming.sort(Comparator.comparingDouble(SimPatient::arrivalMinute).thenComparingInt(SimPatient::id));

        // min-heap of the times at which each doctor next becomes free (all free at minute 0)
        PriorityQueue<Double> doctorFreeAt = new PriorityQueue<>();
        for (int i = 0; i < doctors; i++) {
            doctorFreeAt.add(0.0);
        }

        List<SimPatient> waiting = new ArrayList<>();
        List<PatientOutcome> outcomes = new ArrayList<>(incoming.size());
        int next = 0;

        while (next < incoming.size() || !waiting.isEmpty()) {
            double now = doctorFreeAt.poll(); // the earliest-free doctor takes the next patient

            if (waiting.isEmpty()) {
                // nobody is waiting: the doctor idles until the next arrival
                now = Math.max(now, incoming.get(next).arrivalMinute());
            }
            while (next < incoming.size() && incoming.get(next).arrivalMinute() <= now) {
                waiting.add(incoming.get(next++));
            }

            SimPatient chosen = policy.pick(waiting, now);
            waiting.remove(chosen);
            outcomes.add(new PatientOutcome(chosen, now - chosen.arrivalMinute()));
            doctorFreeAt.add(now + chosen.serviceMinutes());
        }
        return outcomes;
    }
}
