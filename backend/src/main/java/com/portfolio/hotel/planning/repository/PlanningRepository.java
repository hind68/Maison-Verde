package com.portfolio.hotel.planning.repository;

import com.portfolio.hotel.planning.dto.PlanningResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class PlanningRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlanningRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PlanningResponse find(LocalDate start, LocalDate end) {
        Map<Long, MutableRoom> rooms = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT c.id_chambre, c.numero, c.etage, c.etat_operationnel, c.etat_proprete,
                       tc.libelle AS type_chambre, tc.capacite
                  FROM Chambre c
                  JOIN TypeChambre tc ON tc.id_type = c.id_type
                 ORDER BY c.numero
                """, (RowCallbackHandler) rs -> rooms.put(rs.getLong("id_chambre"), new MutableRoom(
                    rs.getLong("id_chambre"), rs.getString("numero"), rs.getInt("etage"),
                    rs.getString("etat_operationnel"), rs.getString("etat_proprete"),
                    rs.getString("type_chambre"), rs.getInt("capacite")
                )));

        jdbcTemplate.query("""
                SELECT r.id_res, r.id_chambre, r.date_deb, r.date_fin, r.statut,
                       r.nb_personnes, cl.prenom, cl.nom
                  FROM Reservation r
                  JOIN Client cl ON cl.id_client = r.id_client
                 WHERE r.date_deb < ?
                   AND r.date_fin > ?
                   AND r.statut <> 'annulée'
                 ORDER BY r.date_deb, r.id_res
                """, rs -> {
            MutableRoom room = rooms.get(rs.getLong("id_chambre"));
            if (room != null) {
                room.stays.add(new PlanningResponse.Stay(
                        rs.getLong("id_res"), rs.getDate("date_deb").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(), rs.getString("statut"),
                        rs.getString("prenom") + " " + rs.getString("nom"),
                        rs.getInt("nb_personnes")
                ));
            }
        }, Date.valueOf(end), Date.valueOf(start));

        return new PlanningResponse(start, end, rooms.values().stream().map(MutableRoom::toResponse).toList());
    }

    private static final class MutableRoom {
        private final long id;
        private final String number;
        private final int floor;
        private final String status;
        private final String housekeepingStatus;
        private final String type;
        private final int capacity;
        private final List<PlanningResponse.Stay> stays = new ArrayList<>();

        private MutableRoom(
                long id, String number, int floor, String status, String housekeepingStatus,
                String type, int capacity
        ) {
            this.id = id;
            this.number = number;
            this.floor = floor;
            this.status = status;
            this.housekeepingStatus = housekeepingStatus;
            this.type = type;
            this.capacity = capacity;
        }

        private PlanningResponse.Room toResponse() {
            return new PlanningResponse.Room(
                    id, number, floor, status, housekeepingStatus, type, capacity, List.copyOf(stays));
        }
    }
}
