package com.hospitalflow.simulation;

import com.hospitalflow.simulation.SimulationResult.WaitStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationServiceTest {

    private final SimulationService service = new SimulationService();

    @Test
    void percentileUsesNearestRank() {
        List<Double> oneToTen = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
        assertEquals(9.0, SimulationService.percentile(oneToTen, 90));
        assertEquals(10.0, SimulationService.percentile(oneToTen, 100));
        assertEquals(5.0, SimulationService.percentile(List.of(5.0), 90));
        assertEquals(0.0, SimulationService.percentile(List.of(), 90));
    }

    @Test
    void statsOfEmptyListAreZeroWithCountZero() {
        WaitStats s = SimulationService.stats(2, List.of());
        assertEquals(0, s.count());
        assertEquals(0.0, s.avgWait());
        assertEquals(0.0, s.p90Wait());
    }

    @Test
    void nullRequestUsesDefaults() {
        SimulationResult result = service.run(null);
        assertEquals(SimulationRequest.DEFAULT_PATIENTS, result.parameters().patients());
        assertEquals(SimulationRequest.DEFAULT_DOCTORS, result.parameters().doctors());
        assertEquals(SimulationRequest.DEFAULT_SEED, result.parameters().seed());
    }

    @Test
    void resultHasFourLevelsPerPolicyPlusOverallAndComparison() {
        SimulationResult result = service.run(SimulationRequest.defaults());
        assertEquals("FCFS", result.fcfs().policy());
        assertEquals("PRIORITY", result.priority().policy());
        assertEquals(4, result.fcfs().byLevel().size());
        assertEquals(4, result.priority().byLevel().size());
        assertEquals(5, result.comparison().size()); // levels 1-4 + overall (level 0)
        assertEquals(0, result.comparison().get(4).triageLevel());
    }

    @Test
    void bothPoliciesServeTheSamePatients() {
        SimulationResult result = service.run(SimulationRequest.defaults());
        for (int i = 0; i < 4; i++) {
            assertEquals(result.fcfs().byLevel().get(i).count(), result.priority().byLevel().get(i).count());
        }
        assertEquals(300 * 20, result.fcfs().overall().count());
    }

    @Test
    void sameSeedGivesIdenticalReport() {
        SimulationRequest request = new SimulationRequest(150, 2, 10.0, 12.0, 5, 123L);
        assertEquals(service.run(request), service.run(request));
    }

    @Test
    void priorityCutsEmergencyWaitComparedWithFcfsUnderLoad() {
        SimulationResult result = service.run(SimulationRequest.defaults());
        WaitStats fcfsL1 = result.fcfs().byLevel().get(0);
        WaitStats prioL1 = result.priority().byLevel().get(0);
        assertTrue(prioL1.avgWait() < fcfsL1.avgWait(),
                "priority avg " + prioL1.avgWait() + " should beat FCFS avg " + fcfsL1.avgWait());
        assertTrue(prioL1.p90Wait() < fcfsL1.p90Wait(),
                "priority p90 " + prioL1.p90Wait() + " should beat FCFS p90 " + fcfsL1.p90Wait());
        assertTrue(result.comparison().get(0).avgImprovementPct() > 0);
    }

    @Test
    void lowestUrgencyPaysForPriorityButIsStillServed() {
        SimulationResult result = service.run(SimulationRequest.defaults());
        WaitStats fcfsL4 = result.fcfs().byLevel().get(3);
        WaitStats prioL4 = result.priority().byLevel().get(3);
        assertTrue(prioL4.avgWait() >= fcfsL4.avgWait());
        assertEquals(fcfsL4.count(), prioL4.count()); // nobody starved out of the report
    }

    @Test
    void offeredLoadReflectsDoctorCount() {
        double threeDoctors = service.run(new SimulationRequest(100, 3, 14.0, 12.0, 3, 1L)).offeredLoad();
        double sixDoctors = service.run(new SimulationRequest(100, 6, 14.0, 12.0, 3, 1L)).offeredLoad();
        assertEquals(threeDoctors / 2.0, sixDoctors, 0.02);
    }
}
