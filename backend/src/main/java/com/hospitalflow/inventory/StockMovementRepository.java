package com.hospitalflow.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop50ByOrderByCreatedAtDescIdDesc();
    List<StockMovement> findByPrescriptionId(Long prescriptionId);
    boolean existsByPrescriptionId(Long prescriptionId);
}
