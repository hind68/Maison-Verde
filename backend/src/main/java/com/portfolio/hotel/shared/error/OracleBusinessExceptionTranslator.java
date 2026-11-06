package com.portfolio.hotel.shared.error;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class OracleBusinessExceptionTranslator {

    public RuntimeException translate(DataAccessException exception) {
        int errorCode = findOracleErrorCode(exception);
        return switch (errorCode) {
            case 20001 -> new ReservationConflictException("La chambre n'est plus disponible pour cette période.");
            case 20002 -> new BusinessValidationException("La capacité de la chambre est insuffisante.");
            case 20003 -> new BusinessValidationException("La date de départ doit être postérieure à la date d'arrivée.");
            case 20004 -> new BusinessValidationException("Les occupants de la réservation sont invalides.");
            case 20005, 30006 -> new ReservationConflictException("La chambre est temporairement indisponible.");
            case 1 -> new ReservationConflictException("La réservation entre en conflit avec des données existantes.");
            case 1403 -> new ResourceNotFoundException("Le client ou la chambre demandé n'existe pas.");
            default -> new IllegalStateException("La réservation n'a pas pu être enregistrée.", exception);
        };
    }

    private int findOracleErrorCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() != 0) {
                return Math.abs(sqlException.getErrorCode());
            }
            current = current.getCause();
        }
        return 0;
    }
}

