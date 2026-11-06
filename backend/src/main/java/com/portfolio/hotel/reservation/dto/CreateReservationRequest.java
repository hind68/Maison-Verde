package com.portfolio.hotel.reservation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Min(1) int adults,
        @Min(0) int children
) {
}

