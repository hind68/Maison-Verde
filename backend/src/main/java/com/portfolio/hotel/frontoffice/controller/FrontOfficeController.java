package com.portfolio.hotel.frontoffice.controller;

import com.portfolio.hotel.frontoffice.dto.ExtendStayRequest;
import com.portfolio.hotel.frontoffice.service.FrontOfficeService;
import com.portfolio.hotel.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reservations")
public class FrontOfficeController {

    private final FrontOfficeService service;

    public FrontOfficeController(FrontOfficeService service) {
        this.service = service;
    }

    @PostMapping("/{id}/check-in")
    public ReservationResponse checkIn(@PathVariable @Positive long id) {
        return service.checkIn(id);
    }

    @PostMapping("/{id}/check-out")
    public ReservationResponse checkOut(@PathVariable @Positive long id) {
        return service.checkOut(id);
    }

    @PatchMapping("/{id}/extend")
    public ReservationResponse extend(
            @PathVariable @Positive long id,
            @Valid @RequestBody ExtendStayRequest request
    ) {
        return service.extend(id, request);
    }
}
