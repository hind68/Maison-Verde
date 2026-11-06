package com.portfolio.hotel.room.repository;

import com.portfolio.hotel.room.dto.RoomCatalogResponse;
import com.portfolio.hotel.shared.dto.PagedResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomCatalogRepository {

    private static final String FROM_AND_FILTERS = """
            FROM Chambre c
            JOIN TypeChambre tc ON tc.id_type = c.id_type
            WHERE (:queryDisabled = 1 OR LOWER(c.numero) LIKE :likeQuery OR LOWER(tc.libelle) LIKE :likeQuery)
              AND (:statusDisabled = 1 OR c.etat_operationnel = :status)
              AND (:housekeepingDisabled = 1 OR c.etat_proprete = :housekeepingStatus)
              AND (:floorDisabled = 1 OR c.etage = :floor)
              AND tc.capacite >= :minCapacity
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RoomCatalogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PagedResponse<RoomCatalogResponse> findAll(
            String query, String status, String housekeepingStatus, Integer floor, int minCapacity, int page, int size
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", query);
        parameters.put("queryDisabled", query.isEmpty() ? 1 : 0);
        parameters.put("likeQuery", "%" + query + "%");
        parameters.put("status", status);
        parameters.put("statusDisabled", status.isEmpty() ? 1 : 0);
        parameters.put("housekeepingStatus", housekeepingStatus);
        parameters.put("housekeepingDisabled", housekeepingStatus.isEmpty() ? 1 : 0);
        parameters.put("floorDisabled", floor == null ? 1 : 0);
        parameters.put("floor", floor == null ? 0 : floor);
        parameters.put("minCapacity", minCapacity);
        parameters.put("offset", page * size);
        parameters.put("size", size);

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM_AND_FILTERS, parameters, Long.class);
        List<RoomCatalogResponse> content = jdbc.query("""
                SELECT c.id_chambre, c.numero, c.etage, c.etat_operationnel, c.etat_proprete,
                       tc.id_type, tc.libelle, tc.capacite
                """ + FROM_AND_FILTERS + """
                ORDER BY c.numero
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """, parameters, (rs, rowNum) -> new RoomCatalogResponse(
                rs.getLong("id_chambre"), rs.getString("numero"), rs.getInt("etage"),
                rs.getString("etat_operationnel"), rs.getString("etat_proprete"),
                rs.getLong("id_type"), rs.getString("libelle"),
                rs.getInt("capacite")
        ));
        return PagedResponse.of(content, page, size, total == null ? 0 : total);
    }

    public RoomCatalogResponse findById(long id) {
        List<RoomCatalogResponse> rooms = jdbc.getJdbcTemplate().query("""
                SELECT c.id_chambre, c.numero, c.etage, c.etat_operationnel, c.etat_proprete,
                       tc.id_type, tc.libelle, tc.capacite
                  FROM Chambre c
                  JOIN TypeChambre tc ON tc.id_type = c.id_type
                 WHERE c.id_chambre = ?
                """, (rs, rowNum) -> new RoomCatalogResponse(
                rs.getLong("id_chambre"), rs.getString("numero"), rs.getInt("etage"),
                rs.getString("etat_operationnel"), rs.getString("etat_proprete"),
                rs.getLong("id_type"), rs.getString("libelle"), rs.getInt("capacite")
        ), id);
        return rooms.isEmpty() ? null : rooms.get(0);
    }
}
