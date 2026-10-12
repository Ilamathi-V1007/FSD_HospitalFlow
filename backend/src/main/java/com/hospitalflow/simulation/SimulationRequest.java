package com.hospitalflow.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** All fields are optional; anything left out falls back to the defaults below. */
public record SimulationRequest(
        @Min(value = 10, message = "patients must be at least 10")
        @Max(value = 2000, message = "patients must be at most 2000")
        Integer patients,

        @Min(value = 1, message = "doctors must be at least 1")
        @Max(value = 20, message = "doctors must be at most 20")
        Integer doctors,

        @DecimalMin(value = "1.0", message = "arrivalsPerHour must be at least 1")
        @DecimalMax(value = "500.0", message = "arrivalsPerHour must be at most 500")
        Double arrivalsPerHour,

        @DecimalMin(value = "3.0", message = "avgConsultMinutes must be at least 3")
        @DecimalMax(value = "60.0", message = "avgConsultMinutes must be at most 60")
        Double avgConsultMinutes,

        @Min(value = 1, message = "replications must be at least 1")
        @Max(value = 50, message = "replications must be at most 50")
        Integer replications,

        Long seed) {

    public static final int DEFAULT_PATIENTS = 300;
    public static final int DEFAULT_DOCTORS = 3;
    public static final double DEFAULT_ARRIVALS_PER_HOUR = 14.0;
    public static final double DEFAULT_AVG_CONSULT_MINUTES = 12.0;
    public static final int DEFAULT_REPLICATIONS = 20;
    public static final long DEFAULT_SEED = 42L;

    public static SimulationRequest defaults() {
        return new SimulationRequest(null, null, null, null, null, null);
    }

    public int patientsOrDefault() { return patients != null ? patients : DEFAULT_PATIENTS; }
    public int doctorsOrDefault() { return doctors != null ? doctors : DEFAULT_DOCTORS; }
    public double arrivalsPerHourOrDefault() { return arrivalsPerHour != null ? arrivalsPerHour : DEFAULT_ARRIVALS_PER_HOUR; }
    public double avgConsultMinutesOrDefault() { return avgConsultMinutes != null ? avgConsultMinutes : DEFAULT_AVG_CONSULT_MINUTES; }
    public int replicationsOrDefault() { return replications != null ? replications : DEFAULT_REPLICATIONS; }
    public long seedOrDefault() { return seed != null ? seed : DEFAULT_SEED; }
}
