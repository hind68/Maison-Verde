package com.portfolio.hotel.client.dto;

public record ClientSummaryResponse(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}

