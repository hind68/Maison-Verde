package com.portfolio.hotel.user.dto;

import com.portfolio.hotel.user.domain.StaffRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(@NotNull StaffRole role, @NotNull Boolean active) {
}
