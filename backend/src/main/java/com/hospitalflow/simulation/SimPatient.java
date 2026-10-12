package com.hospitalflow.simulation;

/** One synthetic patient. Times are in minutes from the start of the simulated day. */
public record SimPatient(int id, int triageLevel, double arrivalMinute, double serviceMinutes) {

    public SimPatient {
        PriorityScorer.requireValidLevel(triageLevel);
        if (arrivalMinute < 0) {
            throw new IllegalArgumentException("arrivalMinute must be >= 0");
        }
        if (serviceMinutes <= 0) {
            throw new IllegalArgumentException("serviceMinutes must be > 0");
        }
    }
}
