package com.hospitalflow.labpharmacy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labpharmacy")
public class LabPharmacyController {

    private final LabRequestRepository labRequestRepository;

    public LabPharmacyController(LabRequestRepository labRequestRepository) {
        this.labRequestRepository = labRequestRepository;
    }

    // TODO (Week 2+): result entry, prescription dispensing, discharge/summary generation
    @GetMapping("/lab-requests/pending")
    public List<LabRequest> getPendingLabRequests() {
        return labRequestRepository.findByStatus(LabRequest.Status.PENDING);
    }
}
