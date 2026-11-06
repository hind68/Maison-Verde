package com.portfolio.hotel.room.repository;

import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.ReservationConflictException;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomOperationsRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoomOperationsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateHousekeeping(long id, String target) {
        RoomState current = lock(id);
        if (current.housekeeping().equals(target)) {
            return;
        }
        boolean valid = ("a_nettoyer".equals(current.housekeeping()) && "nettoyee".equals(target))
                || ("nettoyee".equals(current.housekeeping()) && "prete".equals(target));
        if (!valid) {
            throw new BusinessValidationException("Cette transition de propreté n'est pas autorisée.");
        }
        jdbcTemplate.update("UPDATE Chambre SET etat_proprete = ? WHERE id_chambre = ?", target, id);
    }

    public void updateOperationalStatus(long id, String target) {
        RoomState current = lock(id);
        if (current.operational().equals(target)) {
            return;
        }
        if ("hors_service".equals(target)) {
            Integer blockingStays = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM Reservation
                     WHERE id_chambre = ?
                       AND statut IN ('en_attente', 'confirmée', 'en_cours')
                       AND (statut = 'en_cours' OR date_fin > TRUNC(SYSDATE))
                    """, Integer.class, id);
            if (blockingStays != null && blockingStays > 0) {
                throw new ReservationConflictException(
                        "La chambre possède un séjour actif ou une réservation future et ne peut pas être mise hors service.");
            }
            jdbcTemplate.update("""
                    UPDATE Chambre
                       SET etat_operationnel = 'hors_service', statut = 'maintenance'
                     WHERE id_chambre = ?
                    """, id);
        } else {
            jdbcTemplate.update("""
                    UPDATE Chambre
                       SET etat_operationnel = 'en_service', statut = 'libre'
                     WHERE id_chambre = ?
                    """, id);
        }
    }

    private RoomState lock(long id) {
        List<RoomState> rooms = jdbcTemplate.query("""
                SELECT etat_operationnel, etat_proprete
                  FROM Chambre
                 WHERE id_chambre = ?
                   FOR UPDATE WAIT 10
                """, (rs, rowNum) -> new RoomState(
                rs.getString("etat_operationnel"), rs.getString("etat_proprete")
        ), id);
        if (rooms.isEmpty()) {
            throw new ResourceNotFoundException("La chambre demandée n'existe pas.");
        }
        return rooms.get(0);
    }

    private record RoomState(String operational, String housekeeping) {
    }
}
