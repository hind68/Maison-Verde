package com.portfolio.hotel.availability.repository;

import com.portfolio.hotel.availability.dto.AvailableRoomResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityRepository {

    private static final String AVAILABILITY_SQL = """
            SELECT c.id_chambre, c.numero, c.etage, c.etat_operationnel,
                   tc.id_type, tc.libelle, tc.capacite
              FROM Chambre c
              JOIN TypeChambre tc ON tc.id_type = c.id_type
             WHERE c.etat_operationnel = 'en_service'
               AND tc.capacite >= ?
               AND NOT EXISTS (
                   SELECT 1
                     FROM Reservation r
                    WHERE r.id_chambre = c.id_chambre
                      AND ? < r.date_fin
                      AND ? > r.date_deb
                      AND (? IS NULL OR r.id_res <> ?)
                      AND r.statut IN ('en_attente', 'confirmée', 'en_cours')
               )
             ORDER BY tc.capacite, c.numero
            """;

    private final JdbcTemplate jdbcTemplate;

    public AvailabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AvailableRoomResponse> findAvailable(
            LocalDate checkIn, LocalDate checkOut, int guests, Long excludeReservationId
    ) {
        return jdbcTemplate.query(
                AVAILABILITY_SQL,
                (rs, rowNum) -> new AvailableRoomResponse(
                        rs.getLong("id_chambre"),
                        rs.getString("numero"),
                        rs.getInt("etage"),
                        rs.getString("etat_operationnel"),
                        rs.getLong("id_type"),
                        rs.getString("libelle"),
                        rs.getInt("capacite")
                ),
                guests,
                Date.valueOf(checkIn),
                Date.valueOf(checkOut),
                excludeReservationId,
                excludeReservationId
        );
    }
}
