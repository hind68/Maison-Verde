package com.portfolio.hotel.room.controller;

import com.portfolio.hotel.room.dto.RoomCatalogResponse;
import com.portfolio.hotel.room.dto.UpdateHousekeepingStatusRequest;
import com.portfolio.hotel.room.dto.UpdateOperationalStatusRequest;
import com.portfolio.hotel.room.service.RoomService;
import com.portfolio.hotel.shared.dto.PagedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public PagedResponse<RoomCatalogResponse> findAll(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String housekeepingStatus,
            @RequestParam(required = false) Integer floor,
            @RequestParam(defaultValue = "1") @Min(1) int minCapacity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return roomService.findAll(query, status, housekeepingStatus, floor, minCapacity, page, size);
    }

    @PatchMapping("/{id}/housekeeping")
    public RoomCatalogResponse updateHousekeeping(
            @PathVariable long id,
            @RequestBody @jakarta.validation.Valid UpdateHousekeepingStatusRequest request
    ) {
        return roomService.updateHousekeeping(id, request.status());
    }

    @PatchMapping("/{id}/operational-status")
    public RoomCatalogResponse updateOperationalStatus(
            @PathVariable long id,
            @RequestBody @jakarta.validation.Valid UpdateOperationalStatusRequest request
    ) {
        return roomService.updateOperationalStatus(id, request.status());
    }
}
