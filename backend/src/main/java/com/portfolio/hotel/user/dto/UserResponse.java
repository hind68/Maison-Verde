package com.portfolio.hotel.user.dto;

import com.portfolio.hotel.user.domain.StaffRole;

public record UserResponse(
        long id,
        String login,
        String email,
        String firstName,
        String lastName,
        StaffRole role,
        boolean active,
        boolean mustChangePassword
) {
}
