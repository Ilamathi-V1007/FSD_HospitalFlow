package com.hospitalflow.labpharmacy;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByStatus(Prescription.Status status);

    /** SELECT ... FOR UPDATE: two pharmacists dispensing the same prescription are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prescription p where p.id = :id")
    Optional<Prescription> findByIdForUpdate(@Param("id") Long id);
}
