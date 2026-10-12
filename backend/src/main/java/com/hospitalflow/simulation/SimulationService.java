package com.hospitalflow.simulation;

import com.hospitalflow.simulation.SimulationResult.Comparison;
import com.hospitalflow.simulation.SimulationResult.Parameters;
import com.hospitalflow.simulation.SimulationResult.PolicyResult;
import com.hospitalflow.simulation.SimulationResult.WaitStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the same synthetic arrivals through FCFS and the priority scheduler, repeats that for several
 * seeds ("replications") and pools the waits, then reports average and 90th-percentile wait per
 * triage level. Stateless and database-free.
 */
@Service
public class SimulationService {

    private static final int ALL_LEVELS = 0;

    public SimulationResult run(SimulationRequest request) {
        SimulationRequest r = request != null ? request : SimulationRequest.defaults();
        int patients = r.patientsOrDefault();
        int doctors = r.doctorsOrDefault();
        double arrivalsPerHour = r.arrivalsPerHourOrDefault();
        double avgConsult = r.avgConsultMinutesOrDefault();
        int replications = r.replicationsOrDefault();
        long seed = r.seedOrDefault();

        Map<Integer, List<Double>> fcfsWaits = newBuckets();
        Map<Integer, List<Double>> priorityWaits = newBuckets();
        double totalService = 0.0;
        long totalPatients = 0;

        for (int rep = 0; rep < replications; rep++) {
            List<SimPatient> arrivals = ArrivalGenerator.generate(patients, arrivalsPerHour, avgConsult, seed + rep);
            for (SimPatient p : arrivals) {
                totalService += p.serviceMinutes();
            }
            totalPatients += arrivals.size();

            collect(fcfsWaits, SimulationEngine.run(arrivals, doctors, SchedulingPolicy.FCFS));
            collect(priorityWaits, SimulationEngine.run(arrivals, doctors, SchedulingPolicy.PRIORITY));
        }

        // offered load = arrival rate x mean consultation / doctors. Above 1.0 the queue only grows.
        double meanService = totalPatients == 0 ? 0.0 : totalService / totalPatients;
        double offeredLoad = (arrivalsPerHour / 60.0) * meanService / doctors;

        PolicyResult fcfs = summarise(SchedulingPolicy.FCFS, fcfsWaits);
        PolicyResult priority = summarise(SchedulingPolicy.PRIORITY, priorityWaits);

        return new SimulationResult(
                new Parameters(patients, doctors, arrivalsPerHour, avgConsult, replications, seed),
                round2(offeredLoad),
                fcfs,
                priority,
                compare(fcfs, priority));
    }

    // ---------- helpers ----------

    private static Map<Integer, List<Double>> newBuckets() {
        Map<Integer, List<Double>> buckets = new HashMap<>();
        for (int level = ALL_LEVELS; level <= PriorityScorer.MAX_LEVEL; level++) {
            buckets.put(level, new ArrayList<>());
        }
        return buckets;
    }

    private static void collect(Map<Integer, List<Double>> buckets, List<SimulationEngine.PatientOutcome> outcomes) {
        for (SimulationEngine.PatientOutcome o : outcomes) {
            buckets.get(o.patient().triageLevel()).add(o.waitMinutes());
            buckets.get(ALL_LEVELS).add(o.waitMinutes());
        }
    }

    private static PolicyResult summarise(SchedulingPolicy policy, Map<Integer, List<Double>> buckets) {
        List<WaitStats> byLevel = new ArrayList<>();
        for (int level = PriorityScorer.MIN_LEVEL; level <= PriorityScorer.MAX_LEVEL; level++) {
            byLevel.add(stats(level, buckets.get(level)));
        }
        return new PolicyResult(policy.name(), byLevel, stats(ALL_LEVELS, buckets.get(ALL_LEVELS)));
    }

    static WaitStats stats(int level, List<Double> waits) {
        if (waits.isEmpty()) {
            return new WaitStats(level, 0, 0.0, 0.0, 0.0);
        }
        List<Double> sorted = new ArrayList<>(waits);
        Collections.sort(sorted);
        double sum = 0.0;
        for (double w : sorted) {
            sum += w;
        }
        return new WaitStats(level, sorted.size(), round2(sum / sorted.size()),
                round2(percentile(sorted, 90)), round2(sorted.get(sorted.size() - 1)));
    }

    /** Nearest-rank percentile on an already sorted list. */
    static double percentile(List<Double> sorted, double pct) {
        if (sorted.isEmpty()) {
            return 0.0;
        }
        int rank = (int) Math.ceil(pct / 100.0 * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }

    private static List<Comparison> compare(PolicyResult fcfs, PolicyResult priority) {
        List<Comparison> out = new ArrayList<>();
        for (int i = 0; i < fcfs.byLevel().size(); i++) {
            out.add(compareStats(fcfs.byLevel().get(i), priority.byLevel().get(i)));
        }
        out.add(compareStats(fcfs.overall(), priority.overall()));
        return out;
    }

    private static Comparison compareStats(WaitStats f, WaitStats p) {
        return new Comparison(f.triageLevel(),
                improvementPct(f.avgWait(), p.avgWait()),
                improvementPct(f.p90Wait(), p.p90Wait()));
    }

    private static double improvementPct(double fcfs, double priority) {
        return fcfs <= 0.0 ? 0.0 : round2((fcfs - priority) / fcfs * 100.0);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
