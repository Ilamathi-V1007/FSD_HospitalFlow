package com.patientflow.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.patientflow.FlowIntegrationTest;
import com.patientflow.allocation.AllocationDtos.CompleteRequest;
import com.patientflow.allocation.AllocationDtos.ConsultationDto;
import com.patientflow.common.ApiException;
import com.patientflow.common.AuthUser;
import com.patientflow.queue.VisitStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

/** Doctor availability rules: who may change status, and when a doctor may call a patient. */
class DoctorStatusTest extends FlowIntegrationTest {

    private DoctorStatus statusOf(AuthUser doctor) {
        return consultations.current(doctor).doctor().status();
    }

    @Test
    void busyCannotBeSetByHand() {
        AuthUser doctor = auth(DOCTOR_1);
        ApiException e = assertThrows(ApiException.class, () -> doctorService.setStatus(doctor, DoctorStatus.BUSY));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void doctorCanMoveBetweenAvailableOnBreakAndUnavailable() {
        AuthUser doctor = auth(DOCTOR_1);
        for (DoctorStatus status : new DoctorStatus[] {DoctorStatus.ON_BREAK, DoctorStatus.UNAVAILABLE, DoctorStatus.AVAILABLE}) {
            doctorService.setStatus(doctor, status);
            assertEquals(status, statusOf(doctor));
        }
    }

    @ParameterizedTest
    @EnumSource(value = DoctorStatus.class, names = {"ON_BREAK", "UNAVAILABLE"})
    void doctorWhoIsNotAvailableCannotCallNext(DoctorStatus status) {
        Long visitId = checkIn("Waiting Patient", 2);
        AuthUser doctor = auth(DOCTOR_1);
        doctorService.setStatus(doctor, status);

        ApiException e = assertThrows(ApiException.class, () -> consultations.callNext(doctor));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(VisitStatus.WAITING, queue.getInfo(visitId).status(), "the patient must stay in the queue");
        assertEquals(status, statusOf(doctor), "status must not change");
    }

    @Test
    void callingNextMakesTheDoctorBusyAndLocksTheirStatus() {
        Long visitId = checkIn("First Patient", 2);
        checkIn("Second Patient", 3);
        AuthUser doctor = auth(DOCTOR_1);

        consultations.callNext(doctor);

        assertEquals(DoctorStatus.BUSY, statusOf(doctor));
        assertEquals(VisitStatus.IN_CONSULTATION, queue.getInfo(visitId).status());
        ApiException statusChange = assertThrows(ApiException.class, () -> doctorService.setStatus(doctor, DoctorStatus.AVAILABLE));
        assertEquals(HttpStatus.CONFLICT, statusChange.getStatus());
        ApiException secondCall = assertThrows(ApiException.class, () -> consultations.callNext(doctor));
        assertEquals(HttpStatus.CONFLICT, secondCall.getStatus());
    }

    @Test
    void completingTheConsultationFreesTheDoctor() {
        Long visitId = checkIn("Patient", 2);
        AuthUser doctor = auth(DOCTOR_1);
        ConsultationDto consultation = consultations.callNext(doctor);

        consultations.complete(doctor, consultation.id(), new CompleteRequest("Common cold", null));

        assertEquals(DoctorStatus.AVAILABLE, statusOf(doctor));
        assertEquals(VisitStatus.CONSULTED, queue.getInfo(visitId).status());
        ApiException again = assertThrows(ApiException.class,
                () -> consultations.complete(doctor, consultation.id(), new CompleteRequest("Common cold", null)));
        assertEquals(HttpStatus.CONFLICT, again.getStatus(), "a consultation cannot be completed twice");
    }

    @Test
    void anotherDoctorCannotCompleteSomeonesConsultation() {
        checkIn("Patient", 2);
        ConsultationDto consultation = consultations.callNext(auth(DOCTOR_1));

        ApiException e = assertThrows(ApiException.class,
                () -> consultations.complete(auth(DOCTOR_2), consultation.id(), new CompleteRequest("Wrong doctor", null)));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void callingNextWithAnEmptyQueueIsNotFoundAndLeavesTheDoctorAvailable() {
        AuthUser doctor = auth(DOCTOR_1);

        ApiException e = assertThrows(ApiException.class, () -> consultations.callNext(doctor));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals(DoctorStatus.AVAILABLE, statusOf(doctor));
    }
}
