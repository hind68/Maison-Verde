package com.portfolio.hotel.user.repository;

import com.portfolio.hotel.shared.dto.PagedResponse;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import com.portfolio.hotel.user.domain.StaffAccount;
import com.portfolio.hotel.user.domain.StaffRole;
import com.portfolio.hotel.user.dto.UserResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StaffAccountRepository {

    private static final String ACCOUNT_SELECT = """
            SELECT e.id_employe, e.login, e.email, e.password_hash, e.prenom, e.nom,
                   r.libelle role, e.actif, e.changement_mdp_requis,
                   e.tentatives_connexion, e.verrouille_jusqua
              FROM Employe e
              JOIN RoleEmploye r ON r.id_role = e.id_role
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public StaffAccountRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    public Optional<StaffAccount> findByIdentifier(String identifier) {
        jdbc.update("""
                UPDATE Employe SET tentatives_connexion = 0, verrouille_jusqua = NULL
                 WHERE (LOWER(TRIM(login)) = ? OR LOWER(TRIM(email)) = ?)
                   AND verrouille_jusqua IS NOT NULL
                   AND verrouille_jusqua <= LOCALTIMESTAMP
                """, identifier, identifier);
        List<StaffAccount> accounts = jdbc.query(ACCOUNT_SELECT + """
                 WHERE LOWER(TRIM(e.login)) = ? OR LOWER(TRIM(e.email)) = ?
                """, (rs, rowNum) -> mapAccount(rs), identifier, identifier);
        return accounts.stream().findFirst();
    }

    public Optional<StaffAccount> findById(long id) {
        return jdbc.query(ACCOUNT_SELECT + " WHERE e.id_employe = ?",
                (rs, rowNum) -> mapAccount(rs), id).stream().findFirst();
    }

    public PagedResponse<UserResponse> findAll(String query, int page, int size) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("disabled", query.isBlank() ? 1 : 0);
        parameters.put("query", "%" + query + "%");
        parameters.put("offset", page * size);
        parameters.put("size", size);
        String filters = """
                FROM Employe e JOIN RoleEmploye r ON r.id_role = e.id_role
                WHERE (:disabled = 1 OR LOWER(e.login) LIKE :query OR LOWER(e.email) LIKE :query
                       OR LOWER(e.nom || ' ' || e.prenom) LIKE :query)
                  AND r.libelle IN ('ADMIN', 'MANAGER', 'RECEPTION', 'HOUSEKEEPING')
                """;
        Long total = namedJdbc.queryForObject("SELECT COUNT(*) " + filters, parameters, Long.class);
        List<UserResponse> content = namedJdbc.query("""
                SELECT e.id_employe, e.login, e.email, e.prenom, e.nom, r.libelle role,
                       e.actif, e.changement_mdp_requis
                """ + filters + """
                ORDER BY e.nom, e.prenom, e.id_employe
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """, parameters, (rs, rowNum) -> new UserResponse(
                rs.getLong("id_employe"), rs.getString("login"), rs.getString("email"),
                rs.getString("prenom"), rs.getString("nom"), StaffRole.valueOf(rs.getString("role")),
                rs.getInt("actif") == 1, rs.getInt("changement_mdp_requis") == 1
        ));
        return PagedResponse.of(content, page, size, total == null ? 0 : total);
    }

    public long create(
            String login, String email, String passwordHash, String firstName, String lastName,
            StaffRole role, boolean mustChangePassword
    ) {
        Long id = jdbc.queryForObject("SELECT seq_employe.NEXTVAL FROM dual", Long.class);
        if (id == null) throw new IllegalStateException("Oracle n'a pas retourné d'identifiant employé.");
        int updated = jdbc.update("""
                INSERT INTO Employe(
                  id_employe, nom, prenom, login, email, password_hash, id_role,
                  actif, changement_mdp_requis, tentatives_connexion
                )
                SELECT ?, ?, ?, ?, ?, ?, id_role, 1, ?, 0
                  FROM RoleEmploye WHERE libelle = ?
                """, id, lastName, firstName, login, email, passwordHash,
                mustChangePassword ? 1 : 0, role.name());
        if (updated != 1) throw new IllegalStateException("Le rôle applicatif demandé est introuvable.");
        return id;
    }

    public StaffAccount lockById(long id) {
        List<StaffAccount> accounts = jdbc.query(ACCOUNT_SELECT + """
                 WHERE e.id_employe = ? FOR UPDATE OF e.actif, e.id_role WAIT 10
                """, (rs, rowNum) -> mapAccount(rs), id);
        if (accounts.isEmpty()) throw new ResourceNotFoundException("Le compte demandé n'existe pas.");
        return accounts.get(0);
    }

    public void updateRoleAndActive(long id, StaffRole role, boolean active) {
        int updated = jdbc.update("""
                UPDATE Employe
                   SET id_role = (SELECT id_role FROM RoleEmploye WHERE libelle = ?),
                       actif = ?, modifie_le = SYSTIMESTAMP
                 WHERE id_employe = ?
                """, role.name(), active ? 1 : 0, id);
        if (updated != 1) throw new ResourceNotFoundException("Le compte demandé n'existe pas.");
    }

    public void updatePassword(long id, String hash, boolean mustChange) {
        jdbc.update("""
                UPDATE Employe SET password_hash = ?, changement_mdp_requis = ?,
                       tentatives_connexion = 0, verrouille_jusqua = NULL, modifie_le = SYSTIMESTAMP
                 WHERE id_employe = ?
                """, hash, mustChange ? 1 : 0, id);
    }

    public long countActiveAdmins() {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM Employe e
                JOIN RoleEmploye r ON r.id_role = e.id_role
                WHERE r.libelle = 'ADMIN' AND e.actif = 1
                """, Long.class);
        return count == null ? 0 : count;
    }

    public void recordLoginSuccess(long id) {
        jdbc.update("""
                UPDATE Employe SET tentatives_connexion = 0, verrouille_jusqua = NULL,
                       modifie_le = SYSTIMESTAMP WHERE id_employe = ?
                """, id);
    }

    public void recordLoginFailure(String identifier) {
        jdbc.update("""
                UPDATE Employe
                   SET tentatives_connexion = tentatives_connexion + 1,
                       verrouille_jusqua = CASE WHEN tentatives_connexion + 1 >= 5
                         THEN LOCALTIMESTAMP + INTERVAL '15' MINUTE ELSE verrouille_jusqua END,
                       modifie_le = SYSTIMESTAMP
                 WHERE LOWER(TRIM(login)) = ? OR LOWER(TRIM(email)) = ?
                """, identifier, identifier);
    }

    private StaffAccount mapAccount(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp locked = rs.getTimestamp("verrouille_jusqua");
        return new StaffAccount(
                rs.getLong("id_employe"), rs.getString("login"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("prenom"), rs.getString("nom"),
                StaffRole.valueOf(rs.getString("role")), rs.getInt("actif") == 1,
                rs.getInt("changement_mdp_requis") == 1, rs.getInt("tentatives_connexion"),
                locked == null ? null : locked.toLocalDateTime()
        );
    }
}
