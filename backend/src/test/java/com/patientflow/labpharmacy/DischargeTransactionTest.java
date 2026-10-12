package com.patientflow.labpharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

import com.patientflow.FlowIntegrationTest;
import com.patientflow.allocation.BedStatus;
import com.patientflow.common.ApiException;
import com.patientflow.labpharmacy.LabPharmacyDtos.CompleteLabRequest;
import com.patientflow.labpharmacy.LabPharmacyDtos.CreateLabRequest;
import com.patientflow.labpharmacy.LabPharmacyDtos.CreatePrescriptionRequest;
import com.patientflow.labpharmacy.LabPharmacyDtos.LabRequestDto;
import com.patientflow.labpharmacy.LabPharmacyDtos.SummaryDto;
import com.patientflow.queue.QueueService;
import com.patientflow.queue.VisitStatus;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

/**
 * Discharge is one transaction: summary, bed release and closing the visit succeed or fail together.
 *
 * <p>These tests must not be @Transactional themselves, otherwise they would never see a rollback.
 * The last test probes a race (two discharges at once); if it fails, DischargeService needs to lock
 * the visit first, which is a real bug to fix, not a test problem.
 */
class DischargeTransactionTest extends FlowIntegrationTest {

    @Autowired
    VisitSummaryRepository summaries;

    /** Wraps the real QueueService so a single step can be made to fail. */
    @SpyBean
    QueueService queueSpy;

    private long summaryCount(Long visitId) {
        return summaries.findAll().stream().filter(s -> s.getVisitId().equals(visitId)).count();
    }

    private BedStatus bedStatus(Long bedId) {
        return bedRepository.findById(bedId).orElseThrow().getStatus();
    }

    @Test
    void dischargeWritesTheSummaryFreesTheBedAndClosesTheVisit() {
        Long visitId = visitWithStatus("Happy Path", VisitStatus.CONSULTED);
        Long bedId = availableBedIds().get(0);
        bedService.assign(bedId, visitId);
        prescriptionService.create(auth(DOCTOR_1), new CreatePrescriptionRequest(visitId, "Amoxicillin", "500 mg", "after food"));

        SummaryDto summary = dischargeService.discharge(visitId);

        assertEquals(visitId, summary.visitId());
        assertEquals(VisitStatus.DISCHARGED, queue.getInfo(visitId).status());
        assertEquals(BedStatus.AVAILABLE, bedStatus(bedId));
        assertTrue(bedService.activeBedLabel(visitId).isEmpty());
        assertEquals(1, summaryCount(visitId));
        String prescriptionText = summaries.findAll().stream()
                .filter(s -> s.getVisitId().equals(visitId)).findFirst().orElseThrow().getPrescriptionsText();
        assertTrue(prescriptionText.contains("Amoxicillin"));
    }

    @Test
    void pendingLabResultsBlockDischargeAndChangeNothing() {
        Long visitId = visitWithStatus("Waiting For Lab", VisitStatus.CONSULTED);
        Long bedId = availableBedIds().get(0);
        bedService.assign(bedId, visitId);
        LabRequestDto lab = labService.create(auth(DOCTOR_1), new CreateLabRequest(visitId, "CBC", null));

        ApiException e = assertThrows(ApiException.class, () -> dischargeService.discharge(visitId));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(VisitStatus.CONSULTED, queue.getInfo(visitId).status());
        assertEquals(BedStatus.OCCUPIED, bedStatus(bedId), "the bed must stay held");
        assertEquals(0, summaryCount(visitId), "no summary may be written");

        // once the lab result arrives, the same discharge goes through
        labService.complete(auth(LAB_TECH), lab.id(), new CompleteLabRequest("Normal"));
        dischargeService.discharge(visitId);
        assertEquals(VisitStatus.DISCHARGED, queue.getInfo(visitId).status());
    }

    @Test
    void aVisitStillInConsultationCannotBeDischarged() {
        Long visitId = visitWithStatus("Still With Doctor", VisitStatus.IN_CONSULTATION);

        ApiException e = assertThrows(ApiException.class, () -> dischargeService.discharge(visitId));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(VisitStatus.IN_CONSULTATION, queue.getInfo(visitId).status());
        assertEquals(0, summaryCount(visitId));
    }

    @Test
    void dischargingTwiceIsRefusedAndLeavesOneSummary() {
        Long visitId = visitWithStatus("Twice", VisitStatus.CONSULTED);
        dischargeService.discharge(visitId);

        ApiException e = assertThrows(ApiException.class, () -> dischargeService.discharge(visitId));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(1, summaryCount(visitId));
    }

    @Test
    void everythingRollsBackWhenALaterStepFails() {
        Long visitId = visitWithStatus("Rollback", VisitStatus.CONSULTED);
        Long bedId = availableBedIds().get(0);
        bedService.assign(bedId, visitId);
        // the summary is saved and the bed released before this last step, so a real rollback must undo both
        doThrow(new IllegalStateException("simulated failure")).when(queueSpy).markDischarged(visitId);

        assertThrows(IllegalStateException.class, () -> dischargeService.discharge(visitId));

        assertEquals(VisitStatus.CONSULTED, queue.getInfo(visitId).status(), "visit must not be closed");
        assertEquals(0, summaryCount(visitId), "summary must be rolled back");
        assertEquals(BedStatus.OCCUPIED, bedStatus(bedId), "bed release must be rolled back");
        assertTrue(bedService.activeBedLabel(visitId).isPresent(), "the patient must still hold the bed");
    }

    @Test
    void simultaneousDischargesOfTheSameVisitCreateOnlyOneSummary() throws Exception {
        Long visitId = visitWithStatus("Double Click", VisitStatus.CONSULTED);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> dischargeService.discharge(visitId),
                () -> dischargeService.discharge(visitId)));

        assertEquals(1, outcomes.stream().filter(Objects::isNull).count(), "exactly one discharge should succeed");
        assertEquals(1, summaryCount(visitId));
        Throwable loser = outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow();
        assertEquals(HttpStatus.CONFLICT, assertInstanceOf(ApiException.class, loser,
                "the losing request should get a clean 409, not: " + loser).getStatus());
    }
}
