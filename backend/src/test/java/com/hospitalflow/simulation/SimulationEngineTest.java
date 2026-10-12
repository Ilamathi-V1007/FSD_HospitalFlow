package com.hospitalflow.simulation;

import com.hospitalflow.simulation.SimulationEngine.PatientOutcome;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    private static final double EPS = 1e-9;

    private static Map<Integer, Double> waitsById(List<PatientOutcome> outcomes) {
        Map<Integer, Double> waits = new HashMap<>();
        for (PatientOutcome o : outcomes) {
            waits.put(o.patient().id(), o.waitMinutes());
        }
        return waits;
    }

    @Test
    void emptyArrivalsGiveEmptyResult() {
        assertTrue(SimulationEngine.run(List.of(), 2, SchedulingPolicy.FCFS).isEmpty());
        assertTrue(SimulationEngine.run(List.of(), 2, SchedulingPolicy.PRIORITY).isEmpty());
    }

    @Test
    void needsAtLeastOneDoctor() {
        List<SimPatient> one = List.of(new SimPatient(1, 3, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> SimulationEngine.run(one, 0, SchedulingPolicy.FCFS));
    }

    @Test
    void patientArrivingToIdleDoctorWaitsZero() {
        List<SimPatient> one = List.of(new SimPatient(1, 3, 50, 5));
        assertEquals(0.0, SimulationEngine.run(one, 1, SchedulingPolicy.FCFS).get(0).waitMinutes(), EPS);
    }

    @Test
    void twoDoctorsSeeTwoSimultaneousArrivalsImmediately() {
        List<SimPatient> two = List.of(new SimPatient(1, 3, 0, 10), new SimPatient(2, 3, 0, 10));
        Map<Integer, Double> waits = waitsById(SimulationEngine.run(two, 2, SchedulingPolicy.FCFS));
        assertEquals(0.0, waits.get(1), EPS);
        assertEquals(0.0, waits.get(2), EPS);
    }

    // Doctor is busy with patient 1 until minute 10. Patient 2 (non-urgent) arrived at 1, patient 3 (emergency) at 2.
    private static final List<SimPatient> BUSY_DOCTOR = List.of(
            new SimPatient(1, 4, 0, 10),
            new SimPatient(2, 4, 1, 5),
            new SimPatient(3, 1, 2, 5));

    @Test
    void fcfsServesInArrivalOrder() {
        Map<Integer, Double> waits = waitsById(SimulationEngine.run(BUSY_DOCTOR, 1, SchedulingPolicy.FCFS));
        assertEquals(0.0, waits.get(1), EPS);
        assertEquals(9.0, waits.get(2), EPS);  // starts at 10
        assertEquals(13.0, waits.get(3), EPS); // starts at 15
    }

    @Test
    void priorityLetsTheEmergencyJumpTheQueue() {
        Map<Integer, Double> waits = waitsById(SimulationEngine.run(BUSY_DOCTOR, 1, SchedulingPolicy.PRIORITY));
        assertEquals(0.0, waits.get(1), EPS);
        assertEquals(8.0, waits.get(3), EPS);  // emergency starts at 10
        assertEquals(14.0, waits.get(2), EPS); // non-urgent now starts at 15
    }

    @Test
    void unsortedInputGivesSameResultAsSortedInput() {
        List<SimPatient> shuffled = List.of(BUSY_DOCTOR.get(2), BUSY_DOCTOR.get(0), BUSY_DOCTOR.get(1));
        assertEquals(waitsById(SimulationEngine.run(BUSY_DOCTOR, 1, SchedulingPolicy.PRIORITY)),
                waitsById(SimulationEngine.run(shuffled, 1, SchedulingPolicy.PRIORITY)));
    }

    @Test
    void equalScoresGoToTheEarlierCheckIn() {
        // Doctor is free at minute 21.
        // Patient 2 (L4, arrived 1) has waited 20 -> score 10 + 20 = 30.
        // Patient 3 (L3, arrived 21) has waited 0  -> score 30 + 0  = 30. Exact tie -> earlier check-in (patient 2).
        List<SimPatient> exactTie = List.of(
                new SimPatient(1, 1, 0, 21),
                new SimPatient(2, 4, 1, 5),
                new SimPatient(3, 3, 21, 5));
        Map<Integer, Double> waits = waitsById(SimulationEngine.run(exactTie, 1, SchedulingPolicy.PRIORITY));
        assertEquals(20.0, waits.get(2), EPS); // served first, at minute 21
        assertEquals(5.0, waits.get(3), EPS);  // served at minute 26
    }

    @Test
    void freshLevelThreeStillBeatsLevelFourBeforeTwentyMinutes() {
        // Doctor free at 19: patient 2 (L4, arrived 1) has 10+18=28; patient 3 (L3, arrived 19) has 30.
        List<SimPatient> arrivals = List.of(
                new SimPatient(1, 1, 0, 19),
                new SimPatient(2, 4, 1, 5),
                new SimPatient(3, 3, 19, 5));
        Map<Integer, Double> waits = waitsById(SimulationEngine.run(arrivals, 1, SchedulingPolicy.PRIORITY));
        assertEquals(0.0, waits.get(3), EPS);
        assertEquals(23.0, waits.get(2), EPS);
    }

    @Test
    void everyPatientIsServedExactlyOnceEvenUnderHeavyOverload() {
        // far more arrivals than one doctor can handle: nobody (including level 4) may be lost or starved forever
        List<SimPatient> arrivals = ArrivalGenerator.generate(400, 60, 12, 7L);
        for (SchedulingPolicy policy : SchedulingPolicy.values()) {
            List<PatientOutcome> outcomes = SimulationEngine.run(arrivals, 1, policy);
            assertEquals(arrivals.size(), outcomes.size());
            Set<Integer> ids = new HashSet<>();
            for (PatientOutcome o : outcomes) {
                assertTrue(ids.add(o.patient().id()), "patient served twice: " + o.patient().id());
                assertTrue(o.waitMinutes() >= 0.0);
            }
        }
    }
}
