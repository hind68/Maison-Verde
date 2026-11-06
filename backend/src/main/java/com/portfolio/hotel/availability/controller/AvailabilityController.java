package com.portfolio.hotel.availability.controller;

import com.portfolio.hotel.availability.dto.AvailableRoomResponse;
import com.portfolio.hotel.availability.service.AvailabilityService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rooms")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public List<AvailableRoomResponse> availability(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam @Min(1) int adults,
            @RequestParam(defaultValue = "0") @Min(0) int children,
            @RequestParam(required = false) @Positive Long excludeReservationId
    ) {
        return availabilityService.search(checkIn, checkOut, adults, children, excludeReservationId);
    }
}
