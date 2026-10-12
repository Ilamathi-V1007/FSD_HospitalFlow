package com.patientflow.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.patientflow.FlowIntegrationTest;
import com.patientflow.common.ApiException;
import com.patientflow.common.AuthUser;
import com.patientflow.queue.VisitStatus;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Two requests to call the next patient arrive at the same moment. */
class DoctorConcurrentCallNextTest extends FlowIntegrationTest {

    private DoctorStatus statusOf(AuthUser doctor) {
        return consultations.current(doctor).doctor().status();
    }

    @Test
    void whenOnePatientIsWaitingOnlyOneOfTwoDoctorsGetsThem() throws Exception {
        Long visitId = checkIn("Only Patient", 1);
        AuthUser d1 = auth(DOCTOR_1);
        AuthUser d2 = auth(DOCTOR_2);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> consultations.callNext(d1),
                () -> consultations.callNext(d2)));

        assertEquals(1, outcomes.stream().filter(Objects::isNull).count(), "exactly one doctor should get the patient");
        Throwable loser = outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow();
        ApiException loserError = assertInstanceOf(ApiException.class, loser);
        assertEquals(HttpStatus.NOT_FOUND, loserError.getStatus(), "the other doctor sees an empty queue");
        assertEquals(VisitStatus.IN_CONSULTATION, queue.getInfo(visitId).status());
        long busy = List.of(d1, d2).stream().filter(d -> statusOf(d) == DoctorStatus.BUSY).count();
        assertEquals(1, busy, "only the doctor who got the patient is busy");
    }

    @Test
    void whenTwoPatientsAreWaitingEachDoctorGetsADifferentOne() throws Exception {
        Long alpha = checkIn("Alpha", 1);
        Long beta = checkIn("Beta", 2);
        AuthUser d1 = auth(DOCTOR_1);
        AuthUser d2 = auth(DOCTOR_2);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> consultations.callNext(d1),
                () -> consultations.callNext(d2)));

        assertTrue(outcomes.stream().allMatch(Objects::isNull), "both calls should succeed: " + outcomes);
        Long forD1 = consultations.current(d1).activeConsultation().visit().visitId();
        Long forD2 = consultations.current(d2).activeConsultation().visit().visitId();
        assertNotEquals(forD1, forD2, "two doctors must never get the same patient");
        assertEquals(Set.of(alpha, beta), Set.of(forD1, forD2));
    }

    @Test
    void sameDoctorCallingTwiceAtOnceOnlyStartsOneConsultation() throws Exception {
        Long first = checkIn("First", 1);
        Long second = checkIn("Second", 2);
        AuthUser d1 = auth(DOCTOR_1);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> consultations.callNext(d1),
                () -> consultations.callNext(d1)));

        assertEquals(1, outcomes.stream().filter(Objects::isNull).count(), "a double click must not take two patients");
        Throwable loser = outcomes.stream().filter(Objects::nonNull).findFirst().orElseThrow();
        assertEquals(HttpStatus.CONFLICT, assertInstanceOf(ApiException.class, loser).getStatus());
        assertEquals(VisitStatus.IN_CONSULTATION, queue.getInfo(first).status());
        assertEquals(VisitStatus.WAITING, queue.getInfo(second).status());
    }
}
