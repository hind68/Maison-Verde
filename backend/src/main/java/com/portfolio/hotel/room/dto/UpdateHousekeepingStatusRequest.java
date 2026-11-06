package com.portfolio.hotel.room.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateHousekeepingStatusRequest(@NotBlank String status) {
}
