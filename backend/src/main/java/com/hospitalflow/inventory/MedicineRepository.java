package com.hospitalflow.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findAllByOrderByNameAscStrengthAsc();
    boolean existsByMedicineKey(String medicineKey);

    /** SELECT ... FOR UPDATE: serialises concurrent restocks and dispenses of the same medicine. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicine m where m.id = :id")
    Optional<Medicine> findByIdForUpdate(@Param("id") Long id);
}
