package com.hospitalflow.labpharmacy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitSummaryRepository extends JpaRepository<VisitSummary, Long> {
    boolean existsByAppointmentId(Long appointmentId);
}
