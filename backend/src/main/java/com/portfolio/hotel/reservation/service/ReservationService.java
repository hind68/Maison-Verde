package com.portfolio.hotel.reservation.service;

import com.portfolio.hotel.client.repository.ClientRepository;
import com.portfolio.hotel.reservation.dto.CreateReservationRequest;
import com.portfolio.hotel.reservation.dto.ReservationResponse;
import com.portfolio.hotel.reservation.dto.UpdateReservationRequest;
import com.portfolio.hotel.reservation.repository.ReservationCommandRepository;
import com.portfolio.hotel.reservation.repository.ReservationQueryRepository;
import com.portfolio.hotel.room.repository.RoomRepository;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.OracleBusinessExceptionTranslator;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import com.portfolio.hotel.shared.dto.PagedResponse;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private static final Set<String> STATUSES = Set.of(
            "en_attente", "confirmée", "annulée", "en_cours", "terminée"
    );

    private final ReservationCommandRepository commandRepository;
    private final ReservationQueryRepository queryRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;
    private final OracleBusinessExceptionTranslator exceptionTranslator;

    public ReservationService(
            ReservationCommandRepository commandRepository,
            ReservationQueryRepository queryRepository,
            ClientRepository clientRepository,
            RoomRepository roomRepository,
            OracleBusinessExceptionTranslator exceptionTranslator
    ) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        validateDates(request);
        if (!clientRepository.existsById(request.clientId())) {
            throw new ResourceNotFoundException("Le client demandé n'existe pas.");
        }
        if (!roomRepository.existsById(request.roomId())) {
            throw new ResourceNotFoundException("La chambre demandée n'existe pas.");
        }
        try {
            long id = commandRepository.createConfirmed(request);
            return queryRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("La réservation créée est introuvable."));
        } catch (DataAccessException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(long id) {
        return queryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La réservation demandée n'existe pas."));
    }

    @Transactional
    public ReservationResponse update(long id, UpdateReservationRequest request) {
        validateDates(request.checkIn(), request.checkOut());
        try {
            commandRepository.update(id, request);
            return queryRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("La réservation modifiée est introuvable."));
        } catch (DataAccessException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }

    @Transactional
    public ReservationResponse cancel(long id) {
        try {
            commandRepository.cancel(id);
            return queryRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("La réservation annulée est introuvable."));
        } catch (DataAccessException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> findAll(
            String query, String status, LocalDate checkIn, LocalDate checkOut, int page, int size
    ) {
        if (checkIn != null && checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new BusinessValidationException("La fin de période doit être postérieure au début.");
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) {
            throw new BusinessValidationException("Le statut de réservation demandé est invalide.");
        }
        return queryRepository.findAll(
                normalizedQuery, normalizedStatus, checkIn, checkOut, page, size
        );
    }

    private void validateDates(CreateReservationRequest request) {
        validateDates(request.checkIn(), request.checkOut());
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessValidationException("La date de départ doit être postérieure à la date d'arrivée.");
        }
    }
}
