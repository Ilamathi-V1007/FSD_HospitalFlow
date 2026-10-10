package com.hospitalflow.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByAvailabilityStatus(Doctor.AvailabilityStatus status);
}
