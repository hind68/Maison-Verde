package com.portfolio.hotel.frontoffice.repository;

import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.ReservationConflictException;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FrontOfficeCommandRepository {

    private static final Set<String> EXTENDABLE_STATUSES = Set.of("confirmée", "en_cours");
    private final JdbcTemplate jdbcTemplate;

    public FrontOfficeCommandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void checkIn(long id, LocalDate today) {
        StayLock stay = lockStay(id);
        if (!"confirmée".equals(stay.status()) || stay.actualCheckInPresent()) {
            throw new BusinessValidationException("L'arrivée ne peut être enregistrée que pour une réservation confirmée.");
        }
        if (today.isBefore(stay.checkIn()) || !today.isBefore(stay.checkOut())) {
            throw new BusinessValidationException("L'arrivée doit être enregistrée pendant la période planifiée du séjour.");
        }
        RoomState room = lockRoom(stay.roomId());
        if ("hors_service".equals(room.operational())) {
            throw new ReservationConflictException("La chambre est indisponible pour maintenance.");
        }
        if (!"prete".equals(room.housekeeping())) {
            throw new ReservationConflictException("La chambre doit être inspectée et prête avant l'arrivée.");
        }
        jdbcTemplate.update("""
                UPDATE Reservation
                   SET statut = 'en_cours', date_arrivee_reelle = SYSTIMESTAMP
                 WHERE id_res = ?
                """, id);
    }

    public void checkOut(long id) {
        StayLock stay = lockStay(id);
        if (!"en_cours".equals(stay.status()) || !stay.actualCheckInPresent() || stay.actualCheckOutPresent()) {
            throw new BusinessValidationException("Le départ ne peut être enregistré que pour un séjour en cours.");
        }
        lockRoom(stay.roomId());
        jdbcTemplate.update("""
                UPDATE Reservation
                   SET statut = 'terminée', date_depart_reelle = SYSTIMESTAMP
                 WHERE id_res = ?
                """, id);
        jdbcTemplate.update("UPDATE Chambre SET etat_proprete = 'a_nettoyer' WHERE id_chambre = ?", stay.roomId());
    }

    public void extend(long id, LocalDate newCheckOut) {
        StayLock stay = lockStay(id);
        if (!EXTENDABLE_STATUSES.contains(stay.status())) {
            throw new BusinessValidationException("Ce séjour ne peut pas être prolongé dans son statut actuel.");
        }
        if (!newCheckOut.isAfter(stay.checkOut())) {
            throw new BusinessValidationException("La nouvelle date de départ doit prolonger le séjour existant.");
        }
        RoomState room = lockRoom(stay.roomId());
        if ("hors_service".equals(room.operational())) {
            throw new ReservationConflictException("La chambre est indisponible pour maintenance.");
        }
        Integer overlaps = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Reservation
                 WHERE id_chambre = ?
                   AND id_res <> ?
                   AND ? < date_fin
                   AND ? > date_deb
                   AND statut IN ('en_attente', 'confirmée', 'en_cours')
                """, Integer.class, stay.roomId(), id,
                Date.valueOf(stay.checkIn()), Date.valueOf(newCheckOut));
        if (overlaps != null && overlaps > 0) {
            throw new ReservationConflictException("La chambre n'est pas disponible jusqu'à cette nouvelle date.");
        }
        jdbcTemplate.update("UPDATE Reservation SET date_fin = ? WHERE id_res = ?", Date.valueOf(newCheckOut), id);
    }

    private StayLock lockStay(long id) {
        List<StayLock> stays = jdbcTemplate.query("""
                SELECT statut, date_deb, date_fin, id_chambre,
                       date_arrivee_reelle, date_depart_reelle
                  FROM Reservation
                 WHERE id_res = ?
                   FOR UPDATE WAIT 10
                """, (rs, rowNum) -> new StayLock(
                rs.getString("statut"), rs.getDate("date_deb").toLocalDate(),
                rs.getDate("date_fin").toLocalDate(), rs.getLong("id_chambre"),
                rs.getTimestamp("date_arrivee_reelle") != null,
                rs.getTimestamp("date_depart_reelle") != null
        ), id);
        if (stays.isEmpty()) {
            throw new ResourceNotFoundException("La réservation demandée n'existe pas.");
        }
        return stays.get(0);
    }

    private RoomState lockRoom(long id) {
        List<RoomState> rooms = jdbcTemplate.query("""
                SELECT etat_operationnel, etat_proprete
                  FROM Chambre
                 WHERE id_chambre = ?
                   FOR UPDATE WAIT 10
                """, (rs, rowNum) -> new RoomState(
                rs.getString("etat_operationnel"), rs.getString("etat_proprete")), id);
        if (rooms.isEmpty()) {
            throw new ResourceNotFoundException("La chambre demandée n'existe pas.");
        }
        return rooms.get(0);
    }

    private record StayLock(
            String status,
            LocalDate checkIn,
            LocalDate checkOut,
            long roomId,
            boolean actualCheckInPresent,
            boolean actualCheckOutPresent
    ) {
    }

    private record RoomState(String operational, String housekeeping) {
    }
}
