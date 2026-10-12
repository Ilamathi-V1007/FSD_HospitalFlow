package com.hospitalflow.simulation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrivalGeneratorTest {

    @Test
    void sameSeedGivesIdenticalPatients() {
        assertEquals(ArrivalGenerator.generate(100, 15, 12, 99L), ArrivalGenerator.generate(100, 15, 12, 99L));
    }

    @Test
    void differentSeedGivesDifferentPatients() {
        assertNotEquals(ArrivalGenerator.generate(100, 15, 12, 1L), ArrivalGenerator.generate(100, 15, 12, 2L));
    }

    @Test
    void producesRequestedCountWithSortedArrivalsAndValidFields() {
        List<SimPatient> patients = ArrivalGenerator.generate(500, 20, 12, 5L);
        assertEquals(500, patients.size());
        double previous = 0.0;
        for (SimPatient p : patients) {
            assertTrue(p.arrivalMinute() >= previous);
            assertTrue(p.triageLevel() >= 1 && p.triageLevel() <= 4);
            assertTrue(p.serviceMinutes() >= ArrivalGenerator.MIN_SERVICE_MINUTES);
            previous = p.arrivalMinute();
        }
    }

    @Test
    void triageMixRoughlyFollowsTheConfiguredShares() {
        List<SimPatient> patients = ArrivalGenerator.generate(20000, 20, 12, 11L);
        int[] counts = new int[5];
        for (SimPatient p : patients) {
            counts[p.triageLevel()]++;
        }
        for (int level = 1; level <= 4; level++) {
            assertEquals(ArrivalGenerator.TRIAGE_MIX[level], counts[level] / 20000.0, 0.02);
        }
    }

    @Test
    void zeroPatientsGivesEmptyList() {
        assertTrue(ArrivalGenerator.generate(0, 20, 12, 1L).isEmpty());
    }

    @Test
    void rejectsNonPositiveRates() {
        assertThrows(IllegalArgumentException.class, () -> ArrivalGenerator.generate(10, 0, 12, 1L));
        assertThrows(IllegalArgumentException.class, () -> ArrivalGenerator.generate(10, 10, 0, 1L));
    }
}
