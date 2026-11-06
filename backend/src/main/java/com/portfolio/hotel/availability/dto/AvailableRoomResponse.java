package com.portfolio.hotel.availability.dto;

public record AvailableRoomResponse(
        long id,
        String number,
        int floor,
        String status,
        long roomTypeId,
        String roomType,
        int capacity
) {
}
