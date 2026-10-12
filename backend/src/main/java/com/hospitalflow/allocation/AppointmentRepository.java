package com.hospitalflow.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByStatus(Appointment.Status status);
    List<Appointment> findByDoctor(Doctor doctor);
    Optional<Appointment> findFirstByDoctorAndStatus(Doctor doctor, Appointment.Status status);
}

