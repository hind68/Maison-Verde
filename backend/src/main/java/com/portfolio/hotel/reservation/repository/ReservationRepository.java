package com.portfolio.hotel.reservation.repository;

import com.portfolio.hotel.reservation.domain.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
}

