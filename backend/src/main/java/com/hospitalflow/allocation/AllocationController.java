package com.hospitalflow.allocation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/allocation")
public class AllocationController {

    private final BedRepository bedRepository;

    public AllocationController(BedRepository bedRepository) {
        this.bedRepository = bedRepository;
    }

    // TODO (Week 2+): assign doctor/bed, transactional conflict prevention, discharge release
    @GetMapping("/beds")
    public List<Bed> getAllBeds() {
        return bedRepository.findAll();
    }
}
