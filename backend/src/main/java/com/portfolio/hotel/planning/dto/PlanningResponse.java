package com.portfolio.hotel.planning.dto;

import java.time.LocalDate;
import java.util.List;

public record PlanningResponse(
        LocalDate start,
        LocalDate end,
        List<Room> rooms
) {
    public record Room(
            long id,
            String number,
            int floor,
            String operationalStatus,
            String housekeepingStatus,
            String roomType,
            int capacity,
            List<Stay> reservations
    ) {
    }

    public record Stay(
            long id,
            LocalDate checkIn,
            LocalDate checkOut,
            String status,
            String guestName,
            int guestCount
    ) {
    }
}
