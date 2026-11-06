package com.portfolio.hotel.room.repository;

import com.portfolio.hotel.room.domain.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
}

