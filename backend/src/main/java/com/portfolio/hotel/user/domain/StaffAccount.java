package com.portfolio.hotel.user.domain;

import java.time.LocalDateTime;

public record StaffAccount(
        long id,
        String login,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        StaffRole role,
        boolean active,
        boolean mustChangePassword,
        int failedAttempts,
        LocalDateTime lockedUntil
) {
}
