package com.portfolio.hotel.reservation.controller;

import com.portfolio.hotel.reservation.dto.CreateReservationRequest;
import com.portfolio.hotel.reservation.dto.ReservationResponse;
import com.portfolio.hotel.reservation.dto.UpdateReservationRequest;
import com.portfolio.hotel.reservation.service.ReservationService;
import com.portfolio.hotel.shared.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + response.id())).body(response);
    }

    @GetMapping
    public PagedResponse<ReservationResponse> findAll(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return reservationService.findAll(query, status, checkIn, checkOut, page, size);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable @Positive long id) {
        return reservationService.get(id);
    }

    @PatchMapping("/{id}")
    public ReservationResponse update(
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateReservationRequest request
    ) {
        return reservationService.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable @Positive long id) {
        return reservationService.cancel(id);
    }
}
