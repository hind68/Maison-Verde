package com.portfolio.hotel.frontoffice.service;

import com.portfolio.hotel.frontoffice.dto.ExtendStayRequest;
import com.portfolio.hotel.frontoffice.repository.FrontOfficeCommandRepository;
import com.portfolio.hotel.reservation.dto.ReservationResponse;
import com.portfolio.hotel.reservation.repository.ReservationQueryRepository;
import com.portfolio.hotel.shared.error.OracleBusinessExceptionTranslator;
import java.time.LocalDate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontOfficeService {

    private final FrontOfficeCommandRepository commandRepository;
    private final ReservationQueryRepository queryRepository;
    private final OracleBusinessExceptionTranslator exceptionTranslator;

    public FrontOfficeService(
            FrontOfficeCommandRepository commandRepository,
            ReservationQueryRepository queryRepository,
            OracleBusinessExceptionTranslator exceptionTranslator
    ) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Transactional
    public ReservationResponse checkIn(long id) {
        return execute(id, () -> commandRepository.checkIn(id, LocalDate.now()));
    }

    @Transactional
    public ReservationResponse checkOut(long id) {
        return execute(id, () -> commandRepository.checkOut(id));
    }

    @Transactional
    public ReservationResponse extend(long id, ExtendStayRequest request) {
        return execute(id, () -> commandRepository.extend(id, request.checkOut()));
    }

    private ReservationResponse execute(long id, Runnable command) {
        try {
            command.run();
            return queryRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Le séjour mis à jour est introuvable."));
        } catch (DataAccessException exception) {
            throw exceptionTranslator.translate(exception);
        }
    }
}
