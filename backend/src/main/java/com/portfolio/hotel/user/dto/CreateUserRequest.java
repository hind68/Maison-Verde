package com.portfolio.hotel.user.dto;

import com.portfolio.hotel.user.domain.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String login,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull StaffRole role,
        @NotBlank @Size(min = 12, max = 72) String initialPassword
) {
}
