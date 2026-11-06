package com.portfolio.hotel.reservation.repository;

import com.portfolio.hotel.reservation.dto.ReservationResponse;
import com.portfolio.hotel.shared.dto.PagedResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationQueryRepository {

    private static final String FIND_BY_ID_SQL = """
            SELECT r.id_res, r.date_deb, r.date_fin, r.nb_personnes, r.statut,
                   r.date_arrivee_reelle, r.date_depart_reelle,
                   cl.id_client, cl.prenom, cl.nom, cl.email,
                   ch.id_chambre, ch.numero, ch.etage,
                   tc.libelle AS type_chambre, tc.capacite,
                   NVL((SELECT SUM(pr.quantite)
                          FROM PersonneReservation pr
                          JOIN TypePersonne tp ON tp.id_type_personne = pr.id_type_personne
                         WHERE pr.id_res = r.id_res AND tp.libelle = 'adulte'), 0) AS adultes,
                   NVL((SELECT SUM(pr.quantite)
                          FROM PersonneReservation pr
                          JOIN TypePersonne tp ON tp.id_type_personne = pr.id_type_personne
                         WHERE pr.id_res = r.id_res AND tp.libelle = 'enfant'), 0) AS enfants
              FROM Reservation r
              JOIN Client cl ON cl.id_client = r.id_client
              JOIN Chambre ch ON ch.id_chambre = r.id_chambre
              JOIN TypeChambre tc ON tc.id_type = ch.id_type
             WHERE r.id_res = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ReservationQueryRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    public Optional<ReservationResponse> findById(long id) {
        List<ReservationResponse> results = jdbcTemplate.query(
                FIND_BY_ID_SQL,
                this::map,
                id
        );
        return results.stream().findFirst();
    }

    public PagedResponse<ReservationResponse> findAll(
            String query, String status, LocalDate checkIn, LocalDate checkOut, int page, int size
    ) {
        String filters = """
                FROM Reservation r
                JOIN Client cl ON cl.id_client = r.id_client
                JOIN Chambre ch ON ch.id_chambre = r.id_chambre
                JOIN TypeChambre tc ON tc.id_type = ch.id_type
                WHERE (:queryDisabled = 1
                       OR LOWER(cl.prenom || ' ' || cl.nom) LIKE :likeQuery
                       OR LOWER(cl.nom || ' ' || cl.prenom) LIKE :likeQuery
                       OR LOWER(NVL(cl.email, '')) LIKE :likeQuery
                       OR LOWER(ch.numero) LIKE :likeQuery
                       OR LOWER(tc.libelle) LIKE :likeQuery
                       OR TO_CHAR(r.id_res) = :query)
                  AND (:statusDisabled = 1 OR r.statut = :status)
                  AND (:checkInDisabled = 1 OR r.date_fin > :checkIn)
                  AND (:checkOutDisabled = 1 OR r.date_deb < :checkOut)
                """;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", query);
        parameters.put("queryDisabled", query.isEmpty() ? 1 : 0);
        parameters.put("likeQuery", "%" + query + "%");
        parameters.put("status", status);
        parameters.put("statusDisabled", status.isEmpty() ? 1 : 0);
        parameters.put("checkInDisabled", checkIn == null ? 1 : 0);
        parameters.put("checkIn", checkIn == null ? LocalDate.of(1900, 1, 1) : checkIn);
        parameters.put("checkOutDisabled", checkOut == null ? 1 : 0);
        parameters.put("checkOut", checkOut == null ? LocalDate.of(2999, 12, 31) : checkOut);
        parameters.put("offset", page * size);
        parameters.put("size", size);

        Long total = namedJdbcTemplate.queryForObject("SELECT COUNT(*) " + filters, parameters, Long.class);
        List<ReservationResponse> content = namedJdbcTemplate.query("""
                SELECT r.id_res, r.date_deb, r.date_fin, r.nb_personnes, r.statut,
                       r.date_arrivee_reelle, r.date_depart_reelle,
                       cl.id_client, cl.prenom, cl.nom, cl.email,
                       ch.id_chambre, ch.numero, ch.etage,
                       tc.libelle AS type_chambre, tc.capacite,
                       NVL((SELECT SUM(pr.quantite) FROM PersonneReservation pr
                            JOIN TypePersonne tp ON tp.id_type_personne = pr.id_type_personne
                            WHERE pr.id_res = r.id_res AND tp.libelle = 'adulte'), 0) AS adultes,
                       NVL((SELECT SUM(pr.quantite) FROM PersonneReservation pr
                            JOIN TypePersonne tp ON tp.id_type_personne = pr.id_type_personne
                            WHERE pr.id_res = r.id_res AND tp.libelle = 'enfant'), 0) AS enfants
                """ + filters + """
                ORDER BY r.id_res DESC
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """, parameters, this::map);
        return PagedResponse.of(content, page, size, total == null ? 0 : total);
    }

    private ReservationResponse map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ReservationResponse(
                rs.getLong("id_res"), rs.getDate("date_deb").toLocalDate(),
                rs.getDate("date_fin").toLocalDate(), rs.getInt("nb_personnes"),
                rs.getString("statut"),
                toLocalDateTime(rs.getTimestamp("date_arrivee_reelle")),
                toLocalDateTime(rs.getTimestamp("date_depart_reelle")),
                new ReservationResponse.Occupants(rs.getInt("adultes"), rs.getInt("enfants")),
                new ReservationResponse.Client(rs.getLong("id_client"), rs.getString("prenom"),
                        rs.getString("nom"), rs.getString("email")),
                new ReservationResponse.Room(rs.getLong("id_chambre"), rs.getString("numero"),
                        rs.getInt("etage"), rs.getString("type_chambre"), rs.getInt("capacite"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
