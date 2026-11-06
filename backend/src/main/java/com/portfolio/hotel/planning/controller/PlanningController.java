package com.portfolio.hotel.planning.controller;

import com.portfolio.hotel.planning.dto.PlanningResponse;
import com.portfolio.hotel.planning.service.PlanningService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningService service;

    public PlanningController(PlanningService service) {
        this.service = service;
    }

    @GetMapping
    public PlanningResponse find(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return service.find(start, end);
    }
}
