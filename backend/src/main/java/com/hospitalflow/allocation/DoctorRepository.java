package com.hospitalflow.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByAvailabilityStatus(Doctor.AvailabilityStatus status);
    Optional<Doctor> findByUserId(Long userId);
}

