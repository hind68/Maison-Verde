package com.portfolio.hotel.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record UpdateReservationRequest(
        @NotNull @Positive Long roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @Min(1) int adults,
        @Min(0) int children
) {
}
