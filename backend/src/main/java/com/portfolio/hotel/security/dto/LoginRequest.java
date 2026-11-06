package com.portfolio.hotel.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 200) String identifier,
        @NotBlank @Size(max = 72) String password
) {
}
