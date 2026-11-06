package com.portfolio.hotel.availability.service;

import com.portfolio.hotel.availability.dto.AvailableRoomResponse;
import com.portfolio.hotel.availability.repository.AvailabilityRepository;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableRoomResponse> search(
            LocalDate checkIn, LocalDate checkOut, int adults, int children, Long excludeReservationId
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessValidationException("La date de départ doit être postérieure à la date d'arrivée.");
        }
        return availabilityRepository.findAvailable(checkIn, checkOut, adults + children, excludeReservationId);
    }
}
