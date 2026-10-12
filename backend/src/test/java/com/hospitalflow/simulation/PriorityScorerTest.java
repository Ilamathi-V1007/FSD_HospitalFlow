package com.hospitalflow.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityScorerTest {

    @Test
    void baseWeightsMatchCharter() {
        assertEquals(100.0, PriorityScorer.baseWeight(1));
        assertEquals(60.0, PriorityScorer.baseWeight(2));
        assertEquals(30.0, PriorityScorer.baseWeight(3));
        assertEquals(10.0, PriorityScorer.baseWeight(4));
    }

    @Test
    void scoreAddsOnePointPerWaitingMinute() {
        assertEquals(30.0, PriorityScorer.score(3, 0));
        assertEquals(45.0, PriorityScorer.score(3, 15));
    }

    @Test
    void negativeWaitIsTreatedAsZero() {
        assertEquals(60.0, PriorityScorer.score(2, -5));
    }

    @Test
    void levelFourCatchesFreshLevelThreeAfterTwentyMinutes() {
        assertTrue(PriorityScorer.score(4, 19) < PriorityScorer.score(3, 0));
        assertEquals(PriorityScorer.score(3, 0), PriorityScorer.score(4, 20)); // tie, broken by check-in time
        assertTrue(PriorityScorer.score(4, 21) > PriorityScorer.score(3, 0));
    }

    @Test
    void levelFourCatchesFreshLevelTwoAfterFiftyMinutes() {
        assertTrue(PriorityScorer.score(4, 49) < PriorityScorer.score(2, 0));
        assertEquals(PriorityScorer.score(2, 0), PriorityScorer.score(4, 50));
        assertTrue(PriorityScorer.score(4, 51) > PriorityScorer.score(2, 0));
    }

    @Test
    void invalidLevelsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityScorer.score(0, 5));
        assertThrows(IllegalArgumentException.class, () -> PriorityScorer.score(5, 5));
    }
}
