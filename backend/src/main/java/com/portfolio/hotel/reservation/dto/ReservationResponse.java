package com.portfolio.hotel.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        long id,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        String status,
        LocalDateTime actualCheckIn,
        LocalDateTime actualCheckOut,
        Occupants occupants,
        Client client,
        Room room
) {
    public record Occupants(int adults, int children) {
    }

    public record Client(long id, String firstName, String lastName, String email) {
    }

    public record Room(long id, String number, int floor, String roomType, int capacity) {
    }
}
