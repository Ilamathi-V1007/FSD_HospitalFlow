package com.hospitalflow.simulation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Simulation", description = "FCFS vs. priority scheduler comparison on synthetic arrivals")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run the simulation",
            description = "Runs identical synthetic arrivals through FCFS and the priority scheduler and "
                    + "returns average and 90th-percentile wait per triage level. All body fields are optional.")
    public SimulationResult run(@Valid @RequestBody(required = false) SimulationRequest request) {
        return simulationService.run(request);
    }
}
