package com.portfolio.hotel.frontoffice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExtendStayRequest(@NotNull LocalDate checkOut) {
}
