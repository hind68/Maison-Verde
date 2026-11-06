package com.portfolio.hotel.reservation.repository;

import com.portfolio.hotel.reservation.dto.CreateReservationRequest;
import com.portfolio.hotel.reservation.dto.UpdateReservationRequest;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.ReservationConflictException;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;

@Repository
public class ReservationCommandRepository {

    private static final Set<String> EDITABLE_STATUSES = Set.of("en_attente", "confirmée");

    private static final String CREATE_CALL =
            "{call PKG_RESERVATION_APP.CREER_RESERVATION_CONFIRMEE(?, ?, ?, ?, ?, ?, ?)}";

    private final JdbcTemplate jdbcTemplate;

    public ReservationCommandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createConfirmed(CreateReservationRequest request) {
        Long id = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement statement = connection.prepareCall(CREATE_CALL)) {
                statement.setDate(1, Date.valueOf(request.checkIn()));
                statement.setDate(2, Date.valueOf(request.checkOut()));
                statement.setInt(3, request.adults());
                statement.setInt(4, request.children());
                statement.setLong(5, request.clientId());
                statement.setLong(6, request.roomId());
                statement.registerOutParameter(7, Types.NUMERIC);
                statement.execute();
                return statement.getLong(7);
            }
        });
        if (id == null) {
            throw new IllegalStateException("Oracle n'a retourné aucun identifiant de réservation.");
        }
        return id;
    }

    public void update(long id, UpdateReservationRequest request) {
        String status = lockReservation(id);
        if (!EDITABLE_STATUSES.contains(status)) {
            throw new BusinessValidationException("Cette réservation ne peut plus être modifiée dans son statut actuel.");
        }

        RoomLock room = lockRoom(request.roomId());
        int guests = request.adults() + request.children();
        if ("hors_service".equals(room.status())) {
            throw new ReservationConflictException("La chambre est indisponible pour maintenance.");
        }
        if (guests > room.capacity()) {
            throw new BusinessValidationException("La capacité de la chambre est insuffisante.");
        }
        Integer overlaps = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Reservation
                 WHERE id_chambre = ?
                   AND id_res <> ?
                   AND ? < date_fin
                   AND ? > date_deb
                   AND statut IN ('en_attente', 'confirmée', 'en_cours')
                """, Integer.class, request.roomId(), id,
                Date.valueOf(request.checkIn()), Date.valueOf(request.checkOut()));
        if (overlaps != null && overlaps > 0) {
            throw new ReservationConflictException("La chambre n'est plus disponible pour cette période.");
        }

        if ("confirmée".equals(status)) {
            jdbcTemplate.update("UPDATE Reservation SET statut = 'en_attente' WHERE id_res = ?", id);
        }
        jdbcTemplate.update("""
                UPDATE Reservation
                   SET date_deb = ?, date_fin = ?, nb_personnes = ?, id_chambre = ?
                 WHERE id_res = ?
                """, Date.valueOf(request.checkIn()), Date.valueOf(request.checkOut()),
                guests, request.roomId(), id);
        jdbcTemplate.update("DELETE FROM PersonneReservation WHERE id_res = ?", id);
        long adultType = occupantType("adulte");
        jdbcTemplate.update("""
                INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
                VALUES (?, ?, ?)
                """, id, adultType, request.adults());
        if (request.children() > 0) {
            jdbcTemplate.update("""
                    INSERT INTO PersonneReservation(id_res, id_type_personne, quantite)
                    VALUES (?, ?, ?)
                    """, id, occupantType("enfant"), request.children());
        }
        if ("confirmée".equals(status)) {
            jdbcTemplate.update("UPDATE Reservation SET statut = 'confirmée' WHERE id_res = ?", id);
        }
    }

    public boolean cancel(long id) {
        String status = lockReservation(id);
        if ("annulée".equals(status)) {
            return false;
        }
        if (!EDITABLE_STATUSES.contains(status)) {
            throw new BusinessValidationException("Cette réservation ne peut plus être annulée dans son statut actuel.");
        }
        jdbcTemplate.update("UPDATE Reservation SET statut = 'annulée' WHERE id_res = ?", id);
        return true;
    }

    private String lockReservation(long id) {
        List<String> statuses = jdbcTemplate.query(
                "SELECT statut FROM Reservation WHERE id_res = ? FOR UPDATE WAIT 10",
                (rs, rowNum) -> rs.getString("statut"), id);
        if (statuses.isEmpty()) {
            throw new ResourceNotFoundException("La réservation demandée n'existe pas.");
        }
        return statuses.get(0);
    }

    private RoomLock lockRoom(long id) {
        List<RoomLock> rooms = jdbcTemplate.query("""
                SELECT c.etat_operationnel, tc.capacite
                  FROM Chambre c
                  JOIN TypeChambre tc ON tc.id_type = c.id_type
                 WHERE c.id_chambre = ?
                   FOR UPDATE OF c.etat_operationnel WAIT 10
                """, (rs, rowNum) -> new RoomLock(
                rs.getString("etat_operationnel"), rs.getInt("capacite")), id);
        if (rooms.isEmpty()) {
            throw new ResourceNotFoundException("La chambre demandée n'existe pas.");
        }
        return rooms.get(0);
    }

    private long occupantType(String label) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id_type_personne FROM TypePersonne WHERE libelle = ?", Long.class, label);
        if (id == null) {
            throw new IllegalStateException("Le référentiel des occupants est incomplet.");
        }
        return id;
    }

    private record RoomLock(String status, int capacity) {
    }
}
