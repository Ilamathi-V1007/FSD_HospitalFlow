package com.hospitalflow.labpharmacy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabRequestRepository extends JpaRepository<LabRequest, Long> {
    List<LabRequest> findByStatus(LabRequest.Status status);
}
