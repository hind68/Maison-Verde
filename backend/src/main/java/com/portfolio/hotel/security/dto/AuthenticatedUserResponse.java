package com.portfolio.hotel.security.dto;

import com.portfolio.hotel.security.HotelUserPrincipal;
import com.portfolio.hotel.user.domain.StaffRole;

public record AuthenticatedUserResponse(
        long id,
        String login,
        String email,
        String firstName,
        String lastName,
        StaffRole role,
        boolean mustChangePassword
) {
    public static AuthenticatedUserResponse from(HotelUserPrincipal principal) {
        return new AuthenticatedUserResponse(
                principal.id(), principal.getUsername(), principal.email(), principal.firstName(),
                principal.lastName(), principal.role(), principal.mustChangePassword());
    }
}
