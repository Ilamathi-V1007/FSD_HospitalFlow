package com.patientflow.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.patientflow.FlowIntegrationTest;
import com.patientflow.common.ApiException;
import com.patientflow.queue.VisitStatus;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

/**
 * Edge cases for bed assignment and release.
 *
 * <p>The two concurrency tests at the bottom probe races that the bed row lock does not cover. If
 * they fail, that is a real bug to fix in BedService, not a problem with the test.
 */
class BedEdgeCasesTest extends FlowIntegrationTest {

    private void assertConflict(Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void anOccupiedBedCannotBeAssignedAgain() {
        Long bedId = availableBedIds().get(0);
        Long first = visitWithStatus("First", VisitStatus.IN_CONSULTATION);
        Long second = visitWithStatus("Second", VisitStatus.IN_CONSULTATION);
        bedService.assign(bedId, first);

        assertConflict(() -> bedService.assign(bedId, second));
    }

    @Test
    void aVisitCannotHoldTwoBeds() {
        List<Long> beds = availableBedIds();
        Long visit = visitWithStatus("Greedy", VisitStatus.IN_CONSULTATION);
        bedService.assign(beds.get(0), visit);

        assertConflict(() -> bedService.assign(beds.get(1), visit));
    }

    @ParameterizedTest
    @EnumSource(value = VisitStatus.class, names = {"WAITING", "CANCELLED", "DISCHARGED"})
    void onlyVisitsInOrAfterConsultationCanGetABed(VisitStatus status) {
        Long bedId = availableBedIds().get(0);
        Long visit = visitWithStatus("Not Eligible", status);

        assertConflict(() -> bedService.assign(bedId, visit));
        assertTrue(assignmentRepository.findByReleasedAtIsNull().stream().noneMatch(a -> a.getVisitId().equals(visit)));
    }

    @Test
    void unknownBedAndUnknownVisitAreNotFound() {
        Long visit = visitWithStatus("Real Visit", VisitStatus.IN_CONSULTATION);
        Long bedId = availableBedIds().get(0);

        ApiException noBed = assertThrows(ApiException.class, () -> bedService.assign(999_999L, visit));
        ApiException noVisit = assertThrows(ApiException.class, () -> bedService.assign(bedId, 999_999L));

        assertEquals(HttpStatus.NOT_FOUND, noBed.getStatus());
        assertEquals(HttpStatus.NOT_FOUND, noVisit.getStatus());
    }

    @Test
    void releasingAFreeBedIsRejected() {
        Long bedId = availableBedIds().get(0);

        assertConflict(() -> bedService.release(bedId));
    }

    @Test
    void aReleasedBedCanBeAssignedToSomeoneElse() {
        Long bedId = availableBedIds().get(0);
        Long first = visitWithStatus("Leaving", VisitStatus.IN_CONSULTATION);
        Long second = visitWithStatus("Arriving", VisitStatus.IN_CONSULTATION);

        bedService.assign(bedId, first);
        bedService.release(bedId);
        bedService.assign(bedId, second);

        long activeOnBed = assignmentRepository.findByReleasedAtIsNull().stream().filter(a -> a.getBedId().equals(bedId)).count();
        assertEquals(1, activeOnBed);
        assertTrue(bedService.activeBedLabel(first).isEmpty(), "the first patient no longer holds the bed");
        assertTrue(bedService.activeBedLabel(second).isPresent());
    }

    @Test
    void bedCountsMatchTheActiveAssignments() {
        List<Long> beds = availableBedIds();
        bedService.assign(beds.get(0), visitWithStatus("A", VisitStatus.IN_CONSULTATION));
        bedService.assign(beds.get(1), visitWithStatus("B", VisitStatus.CONSULTED));
        bedService.release(beds.get(0));

        var counts = bedService.counts();

        assertEquals(assignmentRepository.findByReleasedAtIsNull().size(), counts.occupied());
        assertEquals(counts.total(), counts.occupied() + counts.available());
    }

    @Test
    void twoSimultaneousReleasesOfTheSameBedOnlyOneSucceeds() throws Exception {
        Long bedId = availableBedIds().get(0);
        bedService.assign(bedId, visitWithStatus("Patient", VisitStatus.IN_CONSULTATION));

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> bedService.release(bedId),
                () -> bedService.release(bedId)));

        assertEquals(1, outcomes.stream().filter(Objects::isNull).count());
        Throwable loser = outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow();
        assertEquals(HttpStatus.CONFLICT, assertInstanceOf(ApiException.class, loser).getStatus());
    }

    /**
     * Two different beds are locked independently, so the "already has a bed" check can pass for
     * both requests. The database's unique index stops the second one, but the caller should get
     * a clean 409, not a raw database error.
     */
    @Test
    void oneVisitRequestingTwoDifferentBedsAtOnceGetsOnlyOne() throws Exception {
        List<Long> beds = availableBedIds();
        Long visit = visitWithStatus("Double Request", VisitStatus.IN_CONSULTATION);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> bedService.assign(beds.get(0), visit),
                () -> bedService.assign(beds.get(1), visit)));

        assertEquals(1, outcomes.stream().filter(Objects::isNull).count(), "exactly one bed should be assigned");
        long active = assignmentRepository.findByReleasedAtIsNull().stream().filter(a -> a.getVisitId().equals(visit)).count();
        assertEquals(1, active);
        Throwable loser = outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow();
        assertEquals(HttpStatus.CONFLICT, assertInstanceOf(ApiException.class, loser,
                "the losing request should get a clean 409, not: " + loser).getStatus());
    }
}
