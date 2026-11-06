package com.portfolio.hotel.room.dto;

public record RoomCatalogResponse(
        long id,
        String number,
        int floor,
        String operationalStatus,
        String housekeepingStatus,
        long roomTypeId,
        String roomType,
        int capacity
) {
}
